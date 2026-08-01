package com.inuteamflow.server.domain.chat;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.inuteamflow.server.domain.chat.dto.request.ChatMessageSendRequest;
import com.inuteamflow.server.domain.chat.entity.ChatRoom;
import com.inuteamflow.server.domain.chat.entity.ChatRoomMember;
import com.inuteamflow.server.domain.chat.enums.ChatMessageType;
import com.inuteamflow.server.domain.chat.repository.ChatMessageRepository;
import com.inuteamflow.server.domain.chat.repository.ChatRoomMemberRepository;
import com.inuteamflow.server.domain.chat.repository.ChatRoomRepository;
import com.inuteamflow.server.domain.chat.service.ChatMessageService;
import com.inuteamflow.server.domain.notification.enums.NotificationType;
import com.inuteamflow.server.domain.notification.service.NotificationService;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.global.s3.S3Service;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;

/**
 * {@link ChatMessageService}의 메시지 전송 및 FCM 발송 조건을 Mockito 기반 단위 테스트로 검증한다.
 * - Spring Context와 실제 데이터베이스를 사용하지 않는다.
 * - 서비스의 모든 협력 객체를 Mock으로 대체해 분기 로직과 호출 결과만 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @InjectMocks
    ChatMessageService chatMessageService;

    @Mock
    ChatRoomRepository chatRoomRepository;

    @Mock
    ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    ChatMessageRepository chatMessageRepository;

    @Mock
    S3Service s3Service;

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @Mock
    SimpUserRegistry simpUserRegistry;

    @Mock
    NotificationService notificationService;

    private static final Long ROOM_ID = 1L;

    private ChatRoom chatRoom;
    private User sender;
    private User receiver;

    @BeforeEach
    void setUp() {
        chatRoom = mock(ChatRoom.class);
        sender = mock(User.class);
        receiver = mock(User.class);

        when(chatRoom.getChatRoomId()).thenReturn(ROOM_ID);

        when(sender.getUserId()).thenReturn(1L);
        when(sender.getName()).thenReturn("발신자");

        when(receiver.getUserId()).thenReturn(2L);
        when(receiver.getUsername()).thenReturn("receiver");

        ChatRoomMember senderMember = mock(ChatRoomMember.class);
        ChatRoomMember receiverMember = mock(ChatRoomMember.class);
        when(senderMember.getUser()).thenReturn(sender);
        when(receiverMember.getUser()).thenReturn(receiver);

        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));
        when(chatRoomMemberRepository.existsByChatRoomAndUser(chatRoom, sender)).thenReturn(true);
        when(chatRoomMemberRepository.findByChatRoomWithUser(chatRoom))
                .thenReturn(List.of(senderMember, receiverMember));
    }

    @Test
    @DisplayName("수신자 WebSocket 세션이 없으면(앱 꺼짐) FCM을 전송한다")
    void sendFcm_whenReceiverHasNoSession() {
        when(simpUserRegistry.getUser("receiver")).thenReturn(null);
        ChatMessageSendRequest request = textRequest("안녕");

        chatMessageService.sendMessage(ROOM_ID, request, sender);

        verify(notificationService)
                .sendChatFcm(eq(List.of(2L)), eq("발신자"), eq("안녕"), eq(NotificationType.CHAT), anyString(), eq(ROOM_ID));
    }

    @Test
    @DisplayName("수신자가 해당 채팅방을 구독 중이면 FCM을 전송하지 않는다")
    void noFcm_whenReceiverIsSubscribedToThisRoom() {
        SimpUser subscribedUser = mockUserSubscribedTo("/sub/chat-rooms/" + ROOM_ID);
        when(simpUserRegistry.getUser("receiver")).thenReturn(subscribedUser);
        ChatMessageSendRequest request = textRequest("안녕");

        chatMessageService.sendMessage(ROOM_ID, request, sender);

        verify(notificationService, never()).sendChatFcm(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("수신자가 다른 채팅방을 구독 중이면 FCM을 전송한다")
    void sendFcm_whenReceiverIsSubscribedToOtherRoom() {
        SimpUser subscribedUser = mockUserSubscribedTo("/sub/chat-rooms/999");
        when(simpUserRegistry.getUser("receiver")).thenReturn(subscribedUser);
        ChatMessageSendRequest request = textRequest("안녕");

        chatMessageService.sendMessage(ROOM_ID, request, sender);

        verify(notificationService)
                .sendChatFcm(
                        eq(List.of(2L)), anyString(), anyString(), eq(NotificationType.CHAT), anyString(), eq(ROOM_ID));
    }

    @Test
    @DisplayName("이미지 메시지이면 FCM 본문을 '사진을 보냈습니다.'로 전송한다")
    void sendFcm_withFixedBody_whenMessageTypeIsImage() {
        when(simpUserRegistry.getUser("receiver")).thenReturn(null);
        ChatMessageSendRequest request = mock(ChatMessageSendRequest.class);
        when(request.getMessageType()).thenReturn(ChatMessageType.IMAGE);
        when(request.getImageKey()).thenReturn("img-key");

        chatMessageService.sendMessage(ROOM_ID, request, sender);

        verify(notificationService)
                .sendChatFcm(
                        eq(List.of(2L)),
                        anyString(),
                        eq("사진을 보냈습니다."),
                        eq(NotificationType.CHAT),
                        anyString(),
                        eq(ROOM_ID));
    }

    private ChatMessageSendRequest textRequest(String content) {
        ChatMessageSendRequest request = mock(ChatMessageSendRequest.class);
        when(request.getMessageType()).thenReturn(ChatMessageType.TEXT);
        when(request.getContent()).thenReturn(content);
        return request;
    }

    private SimpUser mockUserSubscribedTo(String destination) {
        SimpSubscription subscription = mock(SimpSubscription.class);
        when(subscription.getDestination()).thenReturn(destination);

        SimpSession session = mock(SimpSession.class);
        when(session.getSubscriptions()).thenReturn(Collections.singleton(subscription));

        SimpUser simpUser = mock(SimpUser.class);
        when(simpUser.getSessions()).thenReturn(Collections.singleton(session));

        return simpUser;
    }
}
