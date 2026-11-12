package com.inuteamflow.server.domain.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.inuteamflow.server.domain.notification.enums.NotificationType;
import com.inuteamflow.server.domain.notification.repository.NotificationOptionRepository;
import com.inuteamflow.server.domain.push.dto.PushMessage;
import com.inuteamflow.server.domain.push.entity.PushToken;
import com.inuteamflow.server.domain.push.enums.PushProvider;
import com.inuteamflow.server.domain.push.repository.PushTokenRepository;
import com.inuteamflow.server.domain.push.service.ExpoSender;
import com.inuteamflow.server.domain.push.service.FcmSender;
import com.inuteamflow.server.domain.push.service.PushService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link PushService}가 토큰의 provider에 따라 FCM, Expo 발송 채널로 분기하는지 Mockito 기반 단위 테스트로 검증한다.
 * - 실제 Firebase, Expo 발송은 하지 않고 {@link FcmSender}, {@link ExpoSender}를 mock으로 대체한다.
 */
@ExtendWith(MockitoExtension.class)
class PushSendRoutingTest {

    @InjectMocks
    private PushService pushService;

    @Mock
    private PushTokenRepository pushTokenRepository;

    @Mock
    private NotificationOptionRepository notificationOptionRepository;

    @Mock
    private FcmSender fcmSender;

    @Mock
    private ExpoSender expoSender;

    private static final Long RECEIVER_ID = 1L;

    @Test
    @DisplayName("FCM 토큰은 FcmSender로, Expo 토큰은 ExpoSender로 나누어 발송한다")
    void sendToUser_routesTokensByProvider() {
        when(notificationOptionRepository.findByUserId(RECEIVER_ID)).thenReturn(Optional.empty());
        when(pushTokenRepository.findAllByCreatedBy(RECEIVER_ID))
                .thenReturn(List.of(
                        token("web-token", PushProvider.FCM),
                        token("ExponentPushToken[ios]", PushProvider.EXPO),
                        token("ExponentPushToken[android]", PushProvider.EXPO)));
        when(fcmSender.send(anyList(), any())).thenReturn(List.of());
        when(expoSender.send(anyList(), any())).thenReturn(List.of());

        pushService.sendToUser(RECEIVER_ID, "제목", "내용", "/url", NotificationType.CALENDAR, 10L);

        verify(fcmSender).send(eq(List.of("web-token")), any());
        verify(expoSender).send(eq(List.of("ExponentPushToken[ios]", "ExponentPushToken[android]")), any());
    }

    @Test
    @DisplayName("해당 provider의 토큰이 없으면 그 채널로는 발송하지 않는다")
    void sendToUser_skipsProviderWithoutTokens() {
        when(notificationOptionRepository.findByUserId(RECEIVER_ID)).thenReturn(Optional.empty());
        when(pushTokenRepository.findAllByCreatedBy(RECEIVER_ID))
                .thenReturn(List.of(token("ExponentPushToken[ios]", PushProvider.EXPO)));
        when(expoSender.send(anyList(), any())).thenReturn(List.of());

        pushService.sendToUser(RECEIVER_ID, "제목", "내용", "/url", NotificationType.CALENDAR, 10L);

        verify(fcmSender, never()).send(anyList(), any());
    }

    @Test
    @DisplayName("두 채널에서 등록 해제된 것으로 응답한 토큰을 모아 한 번에 삭제한다")
    void sendToUser_deletesInvalidTokensFromBothProviders() {
        when(notificationOptionRepository.findByUserId(RECEIVER_ID)).thenReturn(Optional.empty());
        when(pushTokenRepository.findAllByCreatedBy(RECEIVER_ID))
                .thenReturn(List.of(
                        token("web-token", PushProvider.FCM), token("ExponentPushToken[ios]", PushProvider.EXPO)));
        when(fcmSender.send(anyList(), any())).thenReturn(List.of("web-token"));
        when(expoSender.send(anyList(), any())).thenReturn(List.of("ExponentPushToken[ios]"));

        pushService.sendToUser(RECEIVER_ID, "제목", "내용", "/url", NotificationType.CALENDAR, 10L);

        verify(pushTokenRepository).deleteByTokenIn(List.of("web-token", "ExponentPushToken[ios]"));
    }

    @Test
    @DisplayName("채팅 알림은 두 채널에 같은 data와 축약 키를 전달한다")
    void sendChatNotification_sendsSameMessageToBothProviders() {
        when(notificationOptionRepository.findByUserIdIn(any())).thenReturn(List.of());
        when(pushTokenRepository.findAllByCreatedByIn(any()))
                .thenReturn(List.of(
                        token("web-token", PushProvider.FCM), token("ExponentPushToken[ios]", PushProvider.EXPO)));
        when(fcmSender.send(anyList(), any())).thenReturn(List.of());
        when(expoSender.send(anyList(), any())).thenReturn(List.of());

        pushService.sendChatNotification(
                List.of(RECEIVER_ID), "발신자", "안녕", NotificationType.CHAT, "/chat/5", 5L, "chat-room-5");

        ArgumentCaptor<PushMessage> fcmMessage = ArgumentCaptor.forClass(PushMessage.class);
        ArgumentCaptor<PushMessage> expoMessage = ArgumentCaptor.forClass(PushMessage.class);
        verify(fcmSender).send(anyList(), fcmMessage.capture());
        verify(expoSender).send(anyList(), expoMessage.capture());

        assertThat(fcmMessage.getValue()).isEqualTo(expoMessage.getValue());
        assertThat(expoMessage.getValue().collapseKey()).isEqualTo("chat-room-5");
        assertThat(expoMessage.getValue().data())
                .containsEntry("redirectUrl", "/chat/5")
                .containsEntry("type", "CHAT")
                .containsEntry("roomId", "5");
    }

    private PushToken token(String token, PushProvider provider) {
        return PushToken.builder()
                .token(token)
                .deviceType("web")
                .provider(provider)
                .build();
    }
}
