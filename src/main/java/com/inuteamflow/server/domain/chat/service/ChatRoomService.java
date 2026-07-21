package com.inuteamflow.server.domain.chat.service;

import com.inuteamflow.server.domain.chat.dto.request.ChatReadRequest;
import com.inuteamflow.server.domain.chat.dto.request.DirectChatRoomCreateRequest;
import com.inuteamflow.server.domain.chat.dto.response.ChatMessageAnchorResponse;
import com.inuteamflow.server.domain.chat.dto.response.ChatMessageResponse;
import com.inuteamflow.server.domain.chat.dto.response.ChatReadEventResponse;
import com.inuteamflow.server.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.inuteamflow.server.domain.chat.entity.ChatMessage;
import com.inuteamflow.server.domain.chat.entity.ChatRoom;
import com.inuteamflow.server.domain.chat.entity.ChatRoomMember;
import com.inuteamflow.server.domain.chat.enums.ChatRoomType;
import com.inuteamflow.server.domain.chat.repository.ChatMessageRepository;
import com.inuteamflow.server.domain.chat.repository.ChatRoomRepository;
import com.inuteamflow.server.domain.chat.repository.ChatRoomMemberRepository;
import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.domain.team.enums.TeamRole;
import com.inuteamflow.server.domain.team.repository.TeamMemberRepository;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private static final int COLLAGE_MAX_MEMBERS = 4;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final SimpMessagingTemplate messagingTemplate;

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

        return ChatMessageAnchorResponse.of(lastReadMessageId, hasMoreBefore, toResponses(chatRoom, combined));
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

        return new SliceImpl<>(toResponses(chatRoom, reversed), pageable, slice.hasNext());
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
                null,
                chatRoom.getChatRoomType(),
                target.getName(),
                s3Service.getImageUrl(target.getImageKey()),
                null,
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

        // 실시간으로 읽음 명수 갱신할 수 있게 브로드캐스트 (누가 읽었는지는 프론트에서 노출하지 않고, 카운트 갱신 트리거로만 사용)
        messagingTemplate.convertAndSend(
                "/sub/chat-rooms/" + roomId + "/read",
                ChatReadEventResponse.of(roomId, user.getUserId(), request.getLastReadMessageId())
        );
    }

    // 팀 채팅방 이미지 설정 (리더만 가능, imageKey=null이면 기본 콜라주로 리셋)
    @Transactional
    public void updateTeamChatRoomImage(User user, Long roomId, String imageKey) {
        ChatRoom chatRoom = getChatRoomById(roomId);

        if (chatRoom.getChatRoomType() != ChatRoomType.TEAM) {
            throw new RestApiException(CustomErrorCode.CHAT_ROOM_INVALID_TARGET);
        }

        TeamMember teamMember = teamMemberRepository.findByTeamAndUser(chatRoom.getTeam(), user)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));

        if (teamMember.getTeamRole() != TeamRole.LEADER) {
            throw new RestApiException(CustomErrorCode.TEAM_FORBIDDEN);
        }

        chatRoom.updateImage(imageKey);
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
        List<String> memberProfileUrls = null;
        Long teamId = null;

        if (chatRoom.getChatRoomType() == ChatRoomType.TEAM) {
            Team team = chatRoom.getTeam();
            teamId = team.getTeamId();
            roomName = team.getName();

            if (chatRoom.getImageKey() != null) {
                // 리더가 커스텀 이미지 설정한 경우
                imageUrl = s3Service.getImageUrl(chatRoom.getImageKey());
            } else {
                // 기본: 멤버 프로필 콜라주용 URL 목록 제공 (프론트에서 조합)
                imageUrl = null;
                memberProfileUrls = chatRoomMemberRepository.findByChatRoomWithUser(chatRoom).stream()
                        .map(ChatRoomMember::getUser)
                        .limit(COLLAGE_MAX_MEMBERS)
                        .map(u -> s3Service.getImageUrl(u.getImageKey()))
                        .toList();
            }
        } else {
            User partner = partnerByRoomId.get(chatRoom.getChatRoomId());
            roomName = partner != null ? partner.getName() : "알 수 없음";
            imageUrl = partner != null ? s3Service.getImageUrl(partner.getImageKey()) : null;
        }

        return ChatRoomSummaryResponse.create(
                chatRoom.getChatRoomId(),
                teamId,
                chatRoom.getChatRoomType(),
                roomName,
                imageUrl,
                memberProfileUrls,
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

    // 메시지 리스트 -> 응답 DTO 리스트 (발신자 배치 조회 + 읽음 명수 계산)
    private List<ChatMessageResponse> toResponses(ChatRoom chatRoom, List<ChatMessage> messages) {
        if (messages.isEmpty()) {
            return List.of();
        }
        Map<Long, User> senderById = userRepository.findAllById(
                messages.stream().map(ChatMessage::getCreatedBy).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getUserId, Function.identity()));

        // 읽음 명수 계산용으로 방 멤버 전체를 한 번만 조회 (N+1 방지)
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomWithUser(chatRoom);

        return messages.stream()
                .map(m -> {
                    User sender = senderById.get(m.getCreatedBy());
                    int readCount = (int) members.stream()
                            .filter(member -> !member.getUser().getUserId().equals(m.getCreatedBy())) // 발신자 본인 제외
                            .filter(member -> member.getLastReadMessageId() != null
                                    && member.getLastReadMessageId() >= m.getChatMessageId())
                            .count();
                    return ChatMessageResponse.of(m, sender, s3Service::getImageUrl, readCount);
                })
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