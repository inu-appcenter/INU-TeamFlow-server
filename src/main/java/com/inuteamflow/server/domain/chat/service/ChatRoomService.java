package com.inuteamflow.server.domain.chat.service;

import com.inuteamflow.server.domain.chat.dto.request.ChatReadRequest;
import com.inuteamflow.server.domain.chat.dto.request.ChatRoomInviteRequest;
import com.inuteamflow.server.domain.chat.dto.request.DirectChatRoomCreateRequest;
import com.inuteamflow.server.domain.chat.dto.request.GroupChatRoomCreateRequest;
import com.inuteamflow.server.domain.chat.dto.response.*;
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
import com.inuteamflow.server.domain.team.repository.TeamRepository;
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
import org.springframework.util.StringUtils;

import java.util.*;
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
    private final ChatMessageService chatMessageService;
    private final TeamRepository teamRepository;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 사용자가 속한 채팅방 목록을 유형별로 조회한다.
     *
     * <p>{@code type}이 {@link ChatRoomType#DIRECT}인 경우에만 대화 상대방 정보를 함께 조회하며,
     * 참여 중인 채팅방이 없으면 빈 목록을 반환한다.</p>
     *
     * @param user 채팅방 목록을 조회할 사용자
     * @param type 조회할 채팅방 유형
     * @return 마지막 메시지 미리보기와 안읽은 메시지 수를 포함한 채팅방 목록
     */
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

    /**
     * 채팅방에 최초 진입했을 때 보여줄 메시지를 조회한다.
     *
     * <p>이전에 읽은 메시지가 없으면 안읽은 메시지 전체만 반환하고, 있으면 마지막으로 읽은 메시지를 기준으로
     * 안읽은 메시지와 이전 맥락 메시지 최대 {@value #CONTEXT_MESSAGE_SIZE}개를 함께 반환한다.
     * 맥락 메시지가 {@value #CONTEXT_MESSAGE_SIZE}개로 꽉 찼다면 그 이전에도 메시지가 더 있을 가능성이 높다고 판단한다.</p>
     *
     * @param roomId 메시지를 조회할 채팅방 ID
     * @param user 조회를 요청한 사용자
     * @return 마지막으로 읽은 메시지 ID, 이전 메시지 존재 여부, 조회된 메시지 목록
     * @throws RestApiException 채팅방을 찾을 수 없거나 사용자가 채팅방 멤버가 아닌 경우
     */
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

    /**
     * 채팅방의 과거 메시지 목록을 커서 기반으로 조회한다.
     *
     * <p>{@code cursor}가 {@code null}이면 가장 최근 메시지부터, 그렇지 않으면 해당 메시지 이전 메시지부터
     * {@code size}개를 조회하여 오래된 순으로 정렬해 반환한다.</p>
     *
     * @param roomId 메시지를 조회할 채팅방 ID
     * @param cursor 이전 메시지 조회 기준이 되는 메시지 ID, 최신 메시지부터 조회할 경우 {@code null}
     * @param size 조회할 메시지 개수
     * @param user 조회를 요청한 사용자
     * @return 오래된 순으로 정렬된 메시지 목록과 다음 페이지 존재 여부
     * @throws RestApiException 채팅방을 찾을 수 없거나 사용자가 채팅방 멤버가 아닌 경우
     */
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

    /**
     * 1:1 채팅방에 진입하거나 없으면 새로 생성한다.
     *
     * <p>본인을 대상으로 요청하면 거절하며, 두 사용자 간 기존 채팅방이 있으면 그 채팅방을 재사용한다.</p>
     *
     * @param me 채팅방에 진입하는 사용자
     * @param request 대화 상대방 정보를 담은 요청
     * @return 상대방 정보와 마지막 메시지 미리보기를 포함한 채팅방 요약
     * @throws RestApiException 대화 상대로 본인을 지정했거나 대상 사용자를 찾을 수 없는 경우
     */
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

    /**
     * 채팅방 메시지를 읽음 처리한다.
     *
     * <p>처리 후 읽음 명수 갱신을 위한 이벤트를 채팅방 구독자에게 브로드캐스트한다.
     * 누가 읽었는지는 노출하지 않고 안읽은 명수 갱신 트리거로만 사용한다.</p>
     *
     * @param roomId 읽음 처리할 채팅방 ID
     * @param user 메시지를 읽은 사용자
     * @param request 마지막으로 읽은 메시지 ID를 담은 요청
     * @throws RestApiException 채팅방을 찾을 수 없거나 사용자가 채팅방 멤버가 아닌 경우
     */
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

    /**
     * 팀 채팅방의 이미지를 설정한다.
     *
     * <p>팀 리더만 변경할 수 있으며, {@code imageKey}가 {@code null}이면 기본 멤버 콜라주 이미지로 초기화된다.</p>
     *
     * @param user 이미지를 변경하는 사용자
     * @param roomId 이미지를 변경할 채팅방 ID
     * @param imageKey 설정할 이미지 키, 기본 콜라주로 되돌릴 경우 {@code null}
     * @throws RestApiException 채팅방을 찾을 수 없거나, 채팅방이 팀 채팅방이 아니거나, 사용자가 팀 멤버가 아니거나,
     *                       팀 리더가 아닌 경우
     */
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

    /**
     * 팀 생성 시 팀 채팅방을 자동으로 생성한다.
     *
     * <p>팀 리더를 채팅방의 첫 멤버로 추가한다.</p>
     *
     * @param team 채팅방을 생성할 팀
     * @param leader 채팅방의 첫 멤버로 추가할 팀 리더
     */
    @Transactional
    public void createTeamChatRoom(Team team, User leader) {
        ChatRoom chatRoom = ChatRoom.createTeamRoom(team);
        chatRoomRepository.save(chatRoom);
        chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom, leader));
    }

    /**
     * 팀 초대 수락 또는 모집 신청 수락 시 팀 채팅방에 사용자를 추가한다.
     *
     * <p>팀 채팅방이 없는 경우(레거시 팀 등)는 조용히 건너뛰며, 이미 채팅방 멤버라면 다시 추가하지 않는다.</p>
     *
     * @param team 멤버를 추가할 팀
     * @param user 채팅방에 추가할 사용자
     */
    @Transactional
    public void addTeamChatRoomMember(Team team, User user) {
        // 채팅방이 없는 경우(레거시 팀 등)는 조용히 스킵
        chatRoomRepository.findByTeamAndChatRoomType(team, ChatRoomType.TEAM).ifPresent(chatRoom -> {
            if (!chatRoomMemberRepository.existsByChatRoomAndUser(chatRoom, user)) {
                chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom, user));
            }
        });
    }

    /**
     * 팀 탈퇴 또는 방출 시 그 팀의 모든 채팅방(팀 채팅방 + 그룹 채팅방)에서 사용자를 제거한다.
     *
     * <p>그룹 채팅방은 제거 후 남은 멤버가 없으면 채팅방과 메시지를 함께 삭제한다. 각 채팅방에서
     * 사용자가 멤버가 아니면 해당 채팅방은 건너뛴다.</p>
     *
     * @param team 멤버를 제거할 팀
     * @param user 채팅방에서 제거할 사용자
     */
    @Transactional
    public void removeTeamChatRoomMember(Team team, User user) {
        for (ChatRoom chatRoom : chatRoomRepository.findAllByTeam(team)) {
            chatRoomMemberRepository.findByChatRoomAndUser(chatRoom, user).ifPresent(member -> {
                chatRoomMemberRepository.delete(member);

                if (chatRoom.getChatRoomType() == ChatRoomType.GROUP
                        && !chatRoomMemberRepository.existsByChatRoom(chatRoom)) {
                    chatMessageRepository.deleteByChatRoom(chatRoom);
                    chatRoomRepository.delete(chatRoom);
                }
            });
        }
    }

    /**
     * 팀 삭제 시 그 팀 채팅방 전체를 삭제한다.
     *
     * <p>멤버, 메시지, 채팅방 순서로 삭제하여 FK 제약을 해소한다.</p>
     *
     * @param team 채팅방을 삭제할 팀
     */
    @Transactional
    public void deleteAllChatRoomsForTeam(Team team) {
        for (ChatRoom chatRoom : chatRoomRepository.findAllByTeam(team)) {
            chatRoomMemberRepository.deleteByChatRoom(chatRoom);
            chatMessageRepository.deleteByChatRoom(chatRoom);
            chatRoomRepository.delete(chatRoom);
        }
    }

    /**
     * 팀 멤버를 선택해 그룹 채팅방을 생성한다.
     *
     * <p>선택한 유저는 모두 해당 팀의 멤버여야 하며, 생성자 본인은 목록에 없어도 자동으로 포함된다.</p>
     *
     * @param creator 채팅방을 생성하는 사용자
     * @param request 생성할 팀 ID, 방 이름, 초대할 팀 멤버 ID 목록
     * @return 생성된 그룹 채팅방 요약 정보
     * @throws RestApiException 팀을 찾을 수 없거나, 생성자가 팀 멤버가 아니거나, 선택한 유저 중 팀 멤버가 아닌 사람이 있는 경우
     */
    @Transactional
    public ChatRoomSummaryResponse createGroupChatRoom(User creator, GroupChatRoomCreateRequest request) {
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));

        teamMemberRepository.findByTeamAndUser(team, creator)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));

        List<Long> memberIds = request.getMemberIds().stream().distinct().toList();
        List<User> selectedUsers = userRepository.findAllById(memberIds);
        validateTeamMembers(team, selectedUsers, memberIds);

        ChatRoom chatRoom = ChatRoom.createGroupRoom(team, request.getRoomName());
        chatRoomRepository.save(chatRoom);

        List<ChatRoomMember> allMembers = new ArrayList<>();
        ChatRoomMember creatorMembership = chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom, creator));
        allMembers.add(creatorMembership);
        for (User user : selectedUsers) {
            if (!user.getUserId().equals(creator.getUserId())) {
                allMembers.add(chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom, user)));
            }
        }

        return toSummary(creatorMembership, null, Map.of(), allMembers);
    }

    /**
     * 그룹 채팅방에 팀 멤버를 추가로 초대한다.
     *
     * <p>그룹 채팅방에서만 가능하며, 현재 채팅방 멤버라면 누구나 초대할 수 있다. 이미 채팅방에 있는 유저는
     * 조용히 건너뛰고, 새로 추가된 인원이 있으면 시스템 메시지로 채팅방에 안내한다.</p>
     *
     * @param user 초대를 요청한 사용자
     * @param roomId 초대할 채팅방 ID
     * @param request 초대할 팀 멤버 ID 목록
     * @throws RestApiException 채팅방을 찾을 수 없거나, 그룹 채팅방이 아니거나, 요청자가 채팅방 멤버가 아니거나,
     *                       선택한 유저 중 팀 멤버가 아닌 사람이 있는 경우
     */
    @Transactional
    public void inviteToGroupChatRoom(User user, Long roomId, ChatRoomInviteRequest request) {
        ChatRoom chatRoom = getChatRoomById(roomId);
        requireGroupRoom(chatRoom);
        getMemberOrThrow(chatRoom, user);

        List<Long> memberIds = request.getMemberIds().stream().distinct().toList();
        List<User> selectedUsers = userRepository.findAllById(memberIds);
        validateTeamMembers(chatRoom.getTeam(), selectedUsers, memberIds);

        Set<Long> existingMemberIds = chatRoomMemberRepository.findByChatRoomWithUser(chatRoom).stream()
                .map(crm -> crm.getUser().getUserId())
                .collect(Collectors.toSet());

        List<User> newMembers = selectedUsers.stream()
                .filter(u -> !existingMemberIds.contains(u.getUserId()))
                .toList();

        for (User newMember : newMembers) {
            chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom, newMember));
        }

        if (!newMembers.isEmpty()) {
            String names = newMembers.stream()
                    .map(u -> u.getName() + "님")
                    .collect(Collectors.joining(", "));
            chatMessageService.sendSystemMessage(chatRoom, names + "이 초대되었습니다.", user);
        }
    }

    /**
     * 그룹 채팅방에서 탈퇴한다.
     *
     * <p>그룹 채팅방에서만 가능하다. 탈퇴 후 남은 멤버가 없으면 채팅방과 메시지를 함께 삭제하고,
     * 남아있으면 탈퇴 사실을 시스템 메시지로 안내한다.</p>
     *
     * @param user 탈퇴할 사용자
     * @param roomId 탈퇴할 채팅방 ID
     * @throws RestApiException 채팅방을 찾을 수 없거나, 그룹 채팅방이 아니거나, 사용자가 채팅방 멤버가 아닌 경우
     */
    @Transactional
    public void leaveGroupChatRoom(User user, Long roomId) {
        ChatRoom chatRoom = getChatRoomById(roomId);
        requireGroupRoom(chatRoom);
        ChatRoomMember member = getMemberOrThrow(chatRoom, user);

        chatRoomMemberRepository.delete(member);

        if (!chatRoomMemberRepository.existsByChatRoom(chatRoom)) {
            chatMessageRepository.deleteByChatRoom(chatRoom);
            chatRoomRepository.delete(chatRoom);
            return;
        }

        chatMessageService.sendSystemMessage(chatRoom, user.getName() + "님이 나갔습니다.", user);
    }

    /**
     * 채팅방의 현재 멤버 목록을 조회한다.
     *
     * <p>TEAM/GROUP 채팅방은 각 멤버의 팀 내 권한을 함께 조회하고, DIRECT 채팅방은 팀이 없으므로
     * 권한 필드를 {@code null}로 둔다.</p>
     *
     * @param user 조회를 요청한 사용자
     * @param roomId 조회할 채팅방 ID
     * @return 학과, 팀 내 권한(1:1이면 {@code null}), 프로필 이미지 URL을 포함한 채팅방 멤버 목록
     * @throws RestApiException 채팅방을 찾을 수 없거나 사용자가 채팅방 멤버가 아닌 경우
     */
    public List<ChatRoomMemberResponse> getChatRoomMembers(User user, Long roomId) {
        ChatRoom chatRoom = getChatRoomById(roomId);
        getMemberOrThrow(chatRoom, user);

        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomWithUser(chatRoom);

        Map<Long, TeamRole> teamRoleByUserId = chatRoom.getChatRoomType() != ChatRoomType.DIRECT
                ? teamMemberRepository.findByTeamAndUserIn(chatRoom.getTeam(), members.stream().map(ChatRoomMember::getUser).toList()).stream()
                .collect(Collectors.toMap(tm -> tm.getUser().getUserId(), TeamMember::getTeamRole))
                : Map.of();

        return members.stream()
                .map(crm -> ChatRoomMemberResponse.create(
                        crm.getUser(),
                        teamRoleByUserId.get(crm.getUser().getUserId()),
                        s3Service.getImageUrl(crm.getUser().getImageKey())
                ))
                .toList();
    }


    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 채팅방 멤버 정보를 채팅방 목록 화면에 표시할 요약 정보로 변환한다.
     *
     * <p>팀 채팅방은 리더가 설정한 이미지가 있으면 그 이미지를, 없으면 멤버 프로필 콜라주용 URL 목록을
     * 최대 {@value #COLLAGE_MAX_MEMBERS}개까지 제공한다. 1:1 채팅방은 상대방의 이름과 프로필 이미지를 사용한다.</p>
     *
     * @param member 요약할 채팅방의 멤버 정보
     * @param lastMessage 채팅방의 마지막 메시지, 없으면 {@code null}
     * @param partnerByRoomId 1:1 채팅방 ID별 상대방 정보
     * @return 채팅방 목록 화면에 표시할 요약 정보
     */
    private ChatRoomSummaryResponse toSummary(ChatRoomMember member, ChatMessage lastMessage, Map<Long, User> partnerByRoomId) {
        return toSummary(member, lastMessage, partnerByRoomId, null);
    }

    private ChatRoomSummaryResponse toSummary(ChatRoomMember member, ChatMessage lastMessage, Map<Long, User> partnerByRoomId, List<ChatRoomMember> preloadedGroupMembers) {
        ChatRoom chatRoom = member.getChatRoom();

        long unreadCount = chatMessageRepository.countByChatRoomAndChatMessageIdGreaterThan(
                chatRoom,
                member.getLastReadMessageId() != null ? member.getLastReadMessageId() : 0L
        );

        String roomName;
        String imageUrl;
        List<String> memberProfileUrls = null;
        Long teamId = null;

        if (chatRoom.getChatRoomType() != ChatRoomType.DIRECT) {
            Team team = chatRoom.getTeam();
            teamId = team.getTeamId();

            if (chatRoom.getChatRoomType() == ChatRoomType.TEAM) {
                roomName = team.getName();
            } else {
                List<ChatRoomMember> groupMembers = preloadedGroupMembers != null
                        ? preloadedGroupMembers
                        : chatRoomMemberRepository.findByChatRoomWithUser(chatRoom);
                roomName = resolveGroupRoomName(chatRoom, groupMembers, member.getUser());
            }

            if (chatRoom.getImageKey() != null) {
                // 리더가 커스텀 이미지 설정한 경우
                imageUrl = s3Service.getImageUrl(chatRoom.getImageKey());
            } else {
                // 기본: 멤버 프로필 콜라주용 URL 목록 제공 (프론트에서 조합)
                imageUrl = null;
                List<ChatRoomMember> membersForCollage = preloadedGroupMembers != null
                        ? preloadedGroupMembers
                        : chatRoomMemberRepository.findByChatRoomWithUser(chatRoom);
                memberProfileUrls = membersForCollage.stream()
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

    /**
     * 마지막 메시지의 미리보기 문자열을 생성한다.
     *
     * @param message 미리보기를 생성할 메시지
     * @return 텍스트/시스템 메시지는 잘라낸 본문, 이미지 메시지는 고정 안내 문구
     */
    private String previewOf(ChatMessage message) {
        return switch (message.getMessageType()) {
            case TEXT, SYSTEM -> truncate(message.getContent());
            case IMAGE -> "사진을 보냈습니다";
        };
    }

    /**
     * 미리보기 텍스트를 최대 길이로 자른다.
     *
     * @param text 자를 대상 텍스트
     * @return 최대 {@value #PREVIEW_MAX_LENGTH}자로 자르고 초과분은 말줄임표로 대체한 텍스트, {@code text}가 {@code null}이면 {@code null}
     */
    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        if (text.length() <= PREVIEW_MAX_LENGTH) {
            return text;
        }
        return text.substring(0, PREVIEW_MAX_LENGTH) + "...";
    }

    /**
     * 1:1 채팅방을 생성하고 두 사용자를 멤버로 추가한다.
     *
     * @param me 채팅방을 생성하는 사용자
     * @param target 대화 상대방
     * @return 생성된 1:1 채팅방
     */
    private ChatRoom createDirectRoom(User me, User target) {
        ChatRoom chatRoom = ChatRoom.createDirectRoom();
        chatRoomRepository.save(chatRoom);

        chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom, me));
        chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom, target));

        return chatRoom;
    }

    /**
     * 메시지 목록을 발신자 정보와 읽음 명수를 포함한 응답 DTO 목록으로 변환한다.
     *
     * <p>발신자 조회와 읽음 명수 계산에 필요한 채팅방 멤버 조회를 각각 한 번씩만 수행하여 N+1 문제를 방지한다.</p>
     *
     * @param chatRoom 메시지가 속한 채팅방
     * @param messages 변환할 메시지 목록
     * @return 발신자 정보와 읽음 명수를 포함한 메시지 응답 목록
     */
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

    /**
     * ID로 채팅방을 조회한다.
     *
     * @param roomId 조회할 채팅방 ID
     * @return 조회된 채팅방
     * @throws RestApiException 채팅방을 찾을 수 없는 경우
     */
    private ChatRoom getChatRoomById(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    /**
     * 채팅방의 멤버 정보를 조회한다.
     *
     * @param chatRoom 멤버를 조회할 채팅방
     * @param user 조회할 사용자
     * @return 조회된 채팅방 멤버 정보
     * @throws RestApiException 사용자가 해당 채팅방 멤버가 아닌 경우
     */
    private ChatRoomMember getMemberOrThrow(ChatRoom chatRoom, User user) {
        return chatRoomMemberRepository.findByChatRoomAndUser(chatRoom, user)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.CHAT_ROOM_FORBIDDEN));
    }

    /**
     * 그룹 채팅방의 표시 이름을 결정한다.
     *
     * <p>커스텀 방 이름이 설정되어 있으면 그대로 사용하고, 없으면 본인을 제외한 참여자 이름 최대 3명과
     * 나머지 인원 수로 구성한 기본 이름을 사용한다.</p>
     *
     * @param chatRoom 이름을 결정할 채팅방
     * @param members 채팅방의 전체 멤버 목록
     * @param viewer 이름을 조회하는 사용자
     * @return 채팅방에 표시할 이름
     */
    private String resolveGroupRoomName(ChatRoom chatRoom, List<ChatRoomMember> members, User viewer) {
        if (StringUtils.hasText(chatRoom.getRoomName())) {
            return chatRoom.getRoomName();
        }

        List<String> otherNames = members.stream()
                .map(ChatRoomMember::getUser)
                .filter(u -> !u.getUserId().equals(viewer.getUserId()))
                .map(User::getName)
                .toList();

        if (otherNames.isEmpty()) {
            return viewer.getName();
        }

        int previewCount = Math.min(3, otherNames.size());
        String preview = String.join(", ", otherNames.subList(0, previewCount));
        int remaining = otherNames.size() - previewCount;

        return remaining > 0 ? preview + " 외 " + remaining + "명" : preview;
    }

    /**
     * 채팅방이 그룹 채팅방인지 확인한다.
     *
     * @param chatRoom 확인할 채팅방
     * @throws RestApiException 채팅방이 그룹 채팅방이 아닌 경우
     */
    private void requireGroupRoom(ChatRoom chatRoom) {
        if (chatRoom.getChatRoomType() != ChatRoomType.GROUP) {
            throw new RestApiException(CustomErrorCode.CHAT_ROOM_INVALID_TARGET);
        }
    }

    /**
     * 선택한 유저 전체가 해당 팀의 멤버인지 검증한다.
     *
     * @param team 검증 기준이 되는 팀
     * @param users 조회된 유저 목록
     * @param requestedIds 요청에 담긴 유저 ID 목록
     * @throws RestApiException 조회된 유저 수가 요청 ID 수와 다르거나, 팀 멤버가 아닌 유저가 포함된 경우
     */
    private void validateTeamMembers(Team team, List<User> users, List<Long> requestedIds) {
        if (users.size() != requestedIds.size()) {
            throw new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND);
        }
        if (teamMemberRepository.findByTeamAndUserIn(team, users).size() != users.size()) {
            throw new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND);
        }
    }
}