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

    // 메시지 전송 (WebSocket 컨트롤러에서 호출)
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

    // 시스템 메시지 생성 + 브로드캐스트
    @Transactional
    public void sendSystemMessage(ChatRoom chatRoom, String content, User triggeredBy) {
        ChatMessage message = ChatMessage.createSystem(chatRoom, content);
        message.assignAuditor(triggeredBy.getUserId());
        chatMessageRepository.save(message);

        broadcast(chatRoom.getChatRoomId(), ChatMessageResponse.of(message, triggeredBy, s3Service::getImageUrl, 0));
    }

    private void broadcast(Long roomId, ChatMessageResponse response) {
        messagingTemplate.convertAndSend("/sub/chat-rooms/" + roomId, response);
    }

    private ChatRoom getChatRoomById(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.CHAT_ROOM_NOT_FOUND));
    }

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
