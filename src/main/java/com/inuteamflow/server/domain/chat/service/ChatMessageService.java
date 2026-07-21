package com.inuteamflow.server.domain.chat.service;

import com.inuteamflow.server.domain.chat.dto.request.ChatMessageSendRequest;
import com.inuteamflow.server.domain.chat.dto.response.ChatMessageResponse;
import com.inuteamflow.server.domain.chat.entity.ChatMessage;
import com.inuteamflow.server.domain.chat.entity.ChatRoom;
import com.inuteamflow.server.domain.chat.repository.ChatMessageRepository;
import com.inuteamflow.server.domain.chat.repository.ChatRoomMemberRepository;
import com.inuteamflow.server.domain.chat.repository.ChatRoomRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import com.inuteamflow.server.global.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final S3Service s3Service;
    private final SimpMessagingTemplate messagingTemplate;

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
        chatMessageRepository.save(message);

        broadcast(roomId, ChatMessageResponse.of(message, sender, s3Service::getImageUrl, 0));
    }

    // 시스템 메시지 생성 + 브로드캐스트
    @Transactional
    public void sendSystemMessage(ChatRoom chatRoom, String content, User triggeredBy) {
        ChatMessage message = ChatMessage.createSystem(chatRoom, content);
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
}
