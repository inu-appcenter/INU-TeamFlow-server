package com.inuteamflow.server.domain.chat.service;

import com.inuteamflow.server.domain.chat.dto.request.ChatReadRequest;
import com.inuteamflow.server.domain.chat.dto.request.DirectChatRoomCreateRequest;
import com.inuteamflow.server.domain.chat.dto.response.ChatMessageAnchorResponse;
import com.inuteamflow.server.domain.chat.dto.response.ChatMessageResponse;
import com.inuteamflow.server.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.inuteamflow.server.domain.chat.entity.ChatMessage;
import com.inuteamflow.server.domain.chat.entity.ChatRoom;
import com.inuteamflow.server.domain.chat.entity.ChatRoomMember;
import com.inuteamflow.server.domain.chat.enums.ChatRoomType;
import com.inuteamflow.server.domain.chat.repository.ChatMessageRepository;
import com.inuteamflow.server.domain.chat.repository.ChatRoomRepository;
import com.inuteamflow.server.domain.chat.repository.ChatRoomMemberRepository;
import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import com.inuteamflow.server.global.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private static final int CONTEXT_MESSAGE_SIZE = 5;
    private static final int PREVIEW_MAX_LENGTH = 30;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    // 내 채팅방 목록 조회
    public List<ChatRoomSummaryResponse> getMyChatRooms(User user, ChatRoomType type) {
        List<ChatRoomMember> myMemberships = chatRoomMemberRepository.findByUserAndChatRoomTypeWithChatRoom(user, type);
        if (myMemberships.isEmpty()) {
            return List.of();
        }

        List<ChatRoom> chatRooms = myMemberships.stream().map(ChatRoomMember::getChatRoom).toList();
        List<Long> chatRoomIds = chatRooms.stream().map(ChatRoom::getChatRoomId).toList();

        Map<Long, ChatMessage> lastMessageByRoomId = chatMessageRepository.findLatestByChatRoomIn(chatRoomIds).stream()
                .collect(Collectors.toMap(m -> m.getChatRoom().getChatRoomId(), Function.identity()));

        // type이 TEAM으로 고정 조회된 경우엔 DIRECT 상대방 조회 자체가 불필요하니 스킵
        Map<Long, User> partnerByRoomId = type == ChatRoomType.DIRECT
                ? chatRoomMemberRepository.findPartnersByChatRoomIdIn(chatRoomIds, user).stream()
                .collect(Collectors.toMap(crm -> crm.getChatRoom().getChatRoomId(), ChatRoomMember::getUser))
                : Map.of();

        return myMemberships.stream()
                .map(member -> toSummary(member, lastMessageByRoomId.get(member.getChatRoom().getChatRoomId()), partnerByRoomId))
                .toList();
    }

    // 채팅방 최초 진입 시 메시지 조회
    public ChatMessageAnchorResponse getMessageInitial(Long roomId, User user) {
        ChatRoom chatRoom = getChatRoomById(roomId);
        ChatRoomMember member = getMemberOrThrow(chatRoom, user);
        Long lastReadMessageId = member.getLastReadMessageId();

        List<ChatMessage> unread;
        List<ChatMessage> context;

        if (lastReadMessageId == null) {
            // 최초 진입 시에는 안읽은 메시지 없음
            unread = chatMessageRepository.findByChatRoomAndChatMessageIdGreaterThanOrderByChatMessageIdAsc(chatRoom, 0L);
            context = List.of();
        } else {
            unread = chatMessageRepository.findByChatRoomAndChatMessageIdGreaterThanOrderByChatMessageIdAsc(chatRoom, lastReadMessageId);
            context = chatMessageRepository.findTop5ByChatRoomAndChatMessageIdLessThanEqualOrderByChatMessageIdDesc(chatRoom, lastReadMessageId);
        }

        List<ChatMessage> combined = new ArrayList<>(context);
        Collections.reverse(combined); // context 는 최신순으로 가져왔으니 오래된 순으로 뒤집기
        combined.addAll(unread); // unread 는 이미 오래된 순

        // 맥락 메시지가 5개 꽉 찼으면 그 이전에도 더 있을 가능선이 높다고 판단 -> 이전 메시지 불러오기
        boolean hasMoreBefore = context.size() == CONTEXT_MESSAGE_SIZE;

        return ChatMessageAnchorResponse.of(lastReadMessageId, hasMoreBefore, toResponses(combined));
    }

    // 히스토리 조회 (과거로 스크롤)
    public Slice<ChatMessageResponse> getMessageHistory(Long roomId, Long cursor, int size, User user) {
        ChatRoom chatRoom = getChatRoomById(roomId);
        getMemberOrThrow(chatRoom, user);

        Pageable pageable = PageRequest.of(0, size);
        Slice<ChatMessage> slice = (cursor == null)
                ? chatMessageRepository.findByChatRoomOrderByChatMessageIdDesc(chatRoom, pageable)
                : chatMessageRepository.findByChatRoomAndChatMessageIdLessThanOrderByChatMessageIdDesc(chatRoom, cursor, pageable);

        List<ChatMessage> reversed = new ArrayList<>(slice.getContent());
        Collections.reverse(reversed); // 오래된순으로 뒤집어서 응답

        return new SliceImpl<>(toResponses(reversed), pageable, slice.hasNext());
    }

    // 1:1 채팅방 진입/생성
    @Transactional
    public ChatRoomSummaryResponse getOrCreateDirectChatRoom(User me, DirectChatRoomCreateRequest request) {
        if (request.getTargetUserId().equals(me.getUserId())) {
            throw new RestApiException(CustomErrorCode.CHAT_ROOM_INVALID_TARGET);
        }

        User target = userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new RestApiException(CustomErrorCode.USER_NOT_FOUND));

        ChatRoom chatRoom = chatRoomMemberRepository.findDirectRoomBetween(me, target, ChatRoomType.DIRECT)
                .orElseGet(() -> createDirectRoom(me, target));

        ChatMessage lastMessage = chatMessageRepository.findLatestByChatRoomIn(List.of(chatRoom.getChatRoomId()))
                .stream().findFirst().orElse(null);

        long unreadCount = chatRoomMemberRepository.findByChatRoomAndUser(chatRoom, me)
                .map(member -> chatMessageRepository.countByChatRoomAndChatMessageIdGreaterThan(
                        chatRoom, member.getLastReadMessageId() != null ? member.getLastReadMessageId() : 0L))
                .orElse(0L);

        return ChatRoomSummaryResponse.create(
                chatRoom.getChatRoomId(),
                chatRoom.getChatRoomType(),
                target.getName(),
                s3Service.getImageUrl(target.getImageKey()),
                lastMessage != null ? previewOf(lastMessage) : null,
                lastMessage != null ? lastMessage.getCreatedAt() : null,
                (int) unreadCount
        );
    }

    // 읽음 처리
    @Transactional
    public void markAsRead(Long roomId, User user, ChatReadRequest request) {
        ChatRoom chatRoom = getChatRoomById(roomId);
        ChatRoomMember member = getMemberOrThrow(chatRoom, user);
        member.updateLastReadMessageId(request.getLastReadMessageId());
    }

    // 팀 생성 시 팀 채팅방 자동 생성 (리더를 첫 멤버로 추가)
    @Transactional
    public void createTeamChatRoom(Team team, User leader) {
        ChatRoom chatRoom = ChatRoom.createTeamRoom(team);
        chatRoomRepository.save(chatRoom);
        chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom, leader));
    }

    // 팀 초대 수락/ 모집 신청 수락 시 팀 채팅방에 멤버 추가
    @Transactional
    public void addTeamChatRoomMember(Team team, User user) {
        // 채팅방이 없는 경우(레거시 팀 등)는 조용히 스킵
        chatRoomRepository.findByTeam(team).ifPresent(chatRoom -> {
            if (!chatRoomMemberRepository.existsByChatRoomAndUser(chatRoom, user)) {
                chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom, user));
            }
        });
    }

    // 팀 탈퇴/ 방출 시 팀 채팅방에서도 제거
    @Transactional
    public void removeTeamChatRoomMember(Team team, User user) {
        chatRoomRepository.findByTeam(team).ifPresent(chatRoom ->
                chatRoomMemberRepository.findByChatRoomAndUser(chatRoom, user)
                        .ifPresent(chatRoomMemberRepository::delete)
        );
    }

    // 팀 삭제 시 그 팀 채팅방 전체 삭제 (멤버 -> 메시지 -> 방 순서)
    @Transactional
    public void deleteTeamChatRoom(Team team) {
        chatRoomRepository.findByTeam(team).ifPresent(chatRoom -> {
            chatRoomMemberRepository.deleteByChatRoom(chatRoom);
            chatMessageRepository.deleteByChatRoom(chatRoom);
            chatRoomRepository.delete(chatRoom);
        });
    }

    /*
     * ===== 헬퍼 함수 =====
     */

    // 채팅방 목록 화면에서 방 하나를 Summary 로 바꾸기
    private ChatRoomSummaryResponse toSummary(ChatRoomMember member, ChatMessage lastMessage, Map<Long, User> partnerByRoomId) {
        ChatRoom chatRoom = member.getChatRoom();

        long unreadCount = chatMessageRepository.countByChatRoomAndChatMessageIdGreaterThan(
                chatRoom,
                member.getLastReadMessageId() != null ? member.getLastReadMessageId() : 0L
        );

        String roomName;
        String imageUrl;
        if (chatRoom.getChatRoomType() == ChatRoomType.TEAM) {
            roomName = chatRoom.getTeam().getName();
            imageUrl = s3Service.getTeamImageUrl(chatRoom.getTeam().getImageKey(), chatRoom.getTeam().getCategory());
        } else {
            User partner = partnerByRoomId.get(chatRoom.getChatRoomId());
            roomName = partner != null ? partner.getName() : "알 수 없음";
            imageUrl = partner != null ? s3Service.getImageUrl(partner.getImageKey()) : null;
        }

        return ChatRoomSummaryResponse.create(
                chatRoom.getChatRoomId(),
                chatRoom.getChatRoomType(),
                roomName,
                imageUrl,
                lastMessage != null ? previewOf(lastMessage) : null,
                lastMessage != null ? lastMessage.getCreatedAt() : null,
                (int) unreadCount
        );
    }

    // 마지막 메시지 미리보기
    private String previewOf(ChatMessage message) {
        return switch (message.getMessageType()) {
            case TEXT, SYSTEM -> truncate(message.getContent());
            case IMAGE -> "사진을 보냈습니다";
        };
    }

    // 미리보기 텍스트 자르기 -> 일단 30자
    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        if (text.length() <= PREVIEW_MAX_LENGTH) {
            return text;
        }
        return text.substring(0, PREVIEW_MAX_LENGTH) + "...";
    }

    private ChatRoom createDirectRoom(User me, User target) {
        ChatRoom chatRoom = ChatRoom.createDirectRoom();
        chatRoomRepository.save(chatRoom);

        chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom, me));
        chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom, target));

        return chatRoom;
    }

    private List<ChatMessageResponse> toResponses(List<ChatMessage> messages) {
        if (messages.isEmpty()) {
            return List.of();
        }
        Map<Long, User> senderById = userRepository.findAllById(
                messages.stream().map(ChatMessage::getCreatedBy).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getUserId, Function.identity()));

        return messages.stream()
                .map(m -> ChatMessageResponse.of(m, senderById.get(m.getCreatedBy()), s3Service::getImageUrl))
                .toList();
    }

    private ChatRoom getChatRoomById(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private ChatRoomMember getMemberOrThrow(ChatRoom chatRoom, User user) {
        return chatRoomMemberRepository.findByChatRoomAndUser(chatRoom, user)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.CHAT_ROOM_FORBIDDEN));
    }
}
