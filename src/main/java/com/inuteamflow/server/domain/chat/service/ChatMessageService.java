package com.inuteamflow.server.domain.chat.service;

import com.inuteamflow.server.domain.chat.dto.request.ChatMessageSendRequest;
import com.inuteamflow.server.domain.chat.dto.response.ChatMessageResponse;
import com.inuteamflow.server.domain.chat.entity.ChatMessage;
import com.inuteamflow.server.domain.chat.entity.ChatRoom;
import com.inuteamflow.server.domain.chat.entity.ChatRoomMember;
import com.inuteamflow.server.domain.chat.enums.ChatMessageType;
import com.inuteamflow.server.domain.chat.repository.ChatMessageRepository;
import com.inuteamflow.server.domain.chat.repository.ChatRoomMemberRepository;
import com.inuteamflow.server.domain.chat.repository.ChatRoomRepository;
import com.inuteamflow.server.domain.notification.enums.NotificationType;
import com.inuteamflow.server.domain.notification.service.NotificationService;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import com.inuteamflow.server.global.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final S3Service s3Service;
    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry simpUserRegistry;
    private final NotificationService notificationService;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 채팅 메시지를 전송한다.
     *
     * <p>WebSocket 컨트롤러에서 호출되며, STOMP는 URL 기반 인가가 자동으로 걸리지 않으므로 방 멤버 여부를
     * 직접 검증한다. {@link ChatMessageType#SYSTEM}은 클라이언트가 직접 보낼 수 없다. 저장 후 방 구독자에게
     * 브로드캐스트하고, 방을 구독하지 않은 멤버에게는 FCM 알림을 보낸다.</p>
     *
     * @param roomId 메시지를 전송할 채팅방 ID
     * @param request 전송할 메시지 정보
     * @param sender 메시지를 보낸 사용자
     * @throws RestApiException 채팅방을 찾을 수 없거나, 사용자가 채팅방 멤버가 아니거나,
     *                       메시지 유형이 {@link ChatMessageType#SYSTEM}인 경우
     */
    @Transactional
    public void sendMessage(Long roomId, ChatMessageSendRequest request, User sender) {
        ChatRoom chatRoom = getChatRoomById(roomId);

        // 방 멤버인지 검증 (STOMP는 URL 기반 인가가 자동으로 안 걸림)
        if (!chatRoomMemberRepository.existsByChatRoomAndUser(chatRoom, sender)) {
            throw new RestApiException(CustomErrorCode.CHAT_ROOM_FORBIDDEN);
        }

        ChatMessage message = switch (request.getMessageType()) {
            case TEXT -> ChatMessage.createText(chatRoom, request.getContent());
            case IMAGE -> ChatMessage.createImage(chatRoom, request.getImageKey());
            case SYSTEM -> throw new RestApiException(CustomErrorCode.CHAT_MESSAGE_TYPE_INVALID); // 클라이언트가 직접 못 보냄
        };
        message.assignAuditor(sender.getUserId()); // SecurityContextHolder 의존 없이 직접 채움
        chatMessageRepository.save(message);

        broadcast(roomId, ChatMessageResponse.of(message, sender, s3Service::getImageUrl, 0));

        // 채팅을 구독하지 않은 인원들에게 FCM 전송
        sendChatFcmIfNeeded(chatRoom, sender, roomId, request);
    }

    /**
     * 시스템 메시지를 생성하고 채팅방에 브로드캐스트한다.
     *
     * @param chatRoom 메시지를 생성할 채팅방
     * @param content 시스템 메시지 내용
     * @param triggeredBy 메시지 생성을 유발한 사용자
     */
    @Transactional
    public void sendSystemMessage(ChatRoom chatRoom, String content, User triggeredBy) {
        ChatMessage message = ChatMessage.createSystem(chatRoom, content);
        message.assignAuditor(triggeredBy.getUserId());
        chatMessageRepository.save(message);

        broadcast(chatRoom.getChatRoomId(), ChatMessageResponse.of(message, triggeredBy, s3Service::getImageUrl, 0));
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 메시지 응답을 채팅방 구독자에게 브로드캐스트한다.
     *
     * @param roomId 브로드캐스트할 채팅방 ID
     * @param response 전송할 메시지 응답
     */
    private void broadcast(Long roomId, ChatMessageResponse response) {
        messagingTemplate.convertAndSend("/sub/chat-rooms/" + roomId, response);
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
     * 채팅방을 구독하지 않고 있는 멤버에게 채팅 FCM 알림을 보낸다.
     *
     * <p>발신자는 대상에서 제외하며, WebSocket 세션이 있더라도 해당 채팅방을 구독 중이 아니면 FCM 대상에 포함한다.
     * 대상이 없으면 알림을 보내지 않으며, 이미지 메시지는 고정 안내 문구를 알림 본문으로 사용한다.</p>
     *
     * @param chatRoom 메시지가 전송된 채팅방
     * @param sender 메시지를 보낸 사용자
     * @param roomId 채팅방 ID
     * @param request 전송된 메시지 정보
     */
    private void sendChatFcmIfNeeded(ChatRoom chatRoom, User sender, Long roomId, ChatMessageSendRequest request) {
        List<Long> fcmTargetIds = chatRoomMemberRepository.findByChatRoomWithUser(chatRoom)
                .stream()
                .map(ChatRoomMember::getUser)
                .filter(u -> !u.getUserId().equals(sender.getUserId())) // 발신자는 제외
                .filter(u -> {
                    SimpUser simpUser = simpUserRegistry.getUser(u.getUsername());
                    // WebSocket 세션이 없는 경우 → FCM 대상
                    // WebSocket 세션이 있는 경우 → 해당 채팅방을 구독 중(화면 보는 중) 이라면 FCM 불필요
                    if (simpUser == null) return true;
                    return simpUser.getSessions().stream()
                            .flatMap(session -> session.getSubscriptions().stream())
                            .noneMatch(sub -> sub.getDestination().equals("/sub/chat-rooms/" + roomId));
                })
                .map(User::getUserId)
                .toList();

        // 전부 이 채팅방을 구독 중이라면 알림 전송은 필요 없음
        if (fcmTargetIds.isEmpty()) return;

        String content = request.getMessageType() == ChatMessageType.IMAGE ? "사진을 보냈습니다." : request.getContent();

        notificationService.sendChatFcm(
                fcmTargetIds,       // FCM을 수신해야 하는 사용자 ID
                sender.getName(),   // 알림 제목: 발신자 실제 이름 (확인 필요)
                content,            // 알림 본문: IMAGE가 아니라면 채팅 내용
                NotificationType.CHAT,
                "/chat/" + roomId,
                roomId              // 같은 방 알림 묶음을 위한 collapseKey
        );
    }
}
