package com.inuteamflow.server.domain.fcm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.inuteamflow.server.domain.fcm.repository.FcmTokenRepository;
import com.inuteamflow.server.domain.fcm.service.FcmService;
import com.inuteamflow.server.domain.notification.entity.NotificationOption;
import com.inuteamflow.server.domain.notification.enums.NotificationType;
import com.inuteamflow.server.domain.notification.repository.NotificationOptionRepository;
import com.inuteamflow.server.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link FcmService}의 FCM 발송이 수신자의 알림 활성화 옵션에 따라 필터링되는지 Mockito 기반 단위 테스트로 검증한다.
 * - Spring Context, 실제 데이터베이스, Firebase를 사용하지 않는다.
 * - 토큰 조회 결과를 빈 목록으로 두어 실제 Firebase 발송 이전 단계(필터링)까지의 분기만 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class FcmFilteringServiceTest {

    @InjectMocks
    private FcmService fcmService;

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Mock
    private NotificationOptionRepository notificationOptionRepository;

    private static final Long RECEIVER_ID = 1L;

    @Test
    @DisplayName("단일 발송: 해당 유형 알림이 비활성화된 수신자에게는 토큰 조회조차 하지 않고 발송을 건너뛴다")
    void sendToUser_skipsWhenTypeDisabled() {
        NotificationOption option = optionWithEnabled(NotificationType.CALENDAR, false);
        when(notificationOptionRepository.findByUserId(RECEIVER_ID)).thenReturn(Optional.of(option));

        fcmService.sendToUser(RECEIVER_ID, "제목", "내용", "/url", NotificationType.CALENDAR, 10L);

        verify(fcmTokenRepository, never()).findFcmTokenByCreatedBy(anyLong());
    }

    @Test
    @DisplayName("단일 발송: 해당 유형 알림이 활성화된 수신자는 필터를 통과해 토큰 조회까지 진행한다")
    void sendToUser_proceedsWhenTypeEnabled() {
        NotificationOption option = optionWithEnabled(NotificationType.CALENDAR, true);
        when(notificationOptionRepository.findByUserId(RECEIVER_ID)).thenReturn(Optional.of(option));
        when(fcmTokenRepository.findFcmTokenByCreatedBy(RECEIVER_ID)).thenReturn(List.of());

        fcmService.sendToUser(RECEIVER_ID, "제목", "내용", "/url", NotificationType.CALENDAR, 10L);

        verify(fcmTokenRepository).findFcmTokenByCreatedBy(RECEIVER_ID);
    }

    @Test
    @DisplayName("단일 발송: 알림 옵션이 없는 수신자는 활성화된 것으로 간주해 토큰 조회까지 진행한다")
    void sendToUser_proceedsWhenOptionAbsent() {
        when(notificationOptionRepository.findByUserId(RECEIVER_ID)).thenReturn(Optional.empty());
        when(fcmTokenRepository.findFcmTokenByCreatedBy(RECEIVER_ID)).thenReturn(List.of());

        fcmService.sendToUser(RECEIVER_ID, "제목", "내용", "/url", NotificationType.CHAT, 10L);

        verify(fcmTokenRepository).findFcmTokenByCreatedBy(RECEIVER_ID);
    }

    @Test
    @DisplayName("다중 발송: 비활성화된 수신자는 제외하고 활성화된 수신자에 대해서만 토큰을 조회한다")
    void sendToUsers_excludesDisabledReceivers() {
        Long enabledId = 1L;
        Long disabledId = 2L;
        NotificationOption enabledOption = optionWithEnabled(NotificationType.NOTICE, true);
        NotificationOption disabledOption = optionForUser(disabledId, NotificationType.NOTICE, false);
        when(notificationOptionRepository.findByUserIdIn(any())).thenReturn(List.of(enabledOption, disabledOption));
        when(fcmTokenRepository.findFcmTokenByCreatedByIn(any())).thenReturn(List.of());

        fcmService.sendToUsers(List.of(enabledId, disabledId), "제목", "내용", "/url", NotificationType.NOTICE);

        verify(fcmTokenRepository).findFcmTokenByCreatedByIn(List.of(enabledId));
    }

    @Test
    @DisplayName("채팅 발송: 비활성화된 수신자는 제외하고 활성화된 수신자에 대해서만 토큰을 조회한다")
    void sendChatNotification_excludesDisabledReceivers() {
        Long enabledId = 1L;
        Long disabledId = 2L;
        NotificationOption enabledOption = optionWithEnabled(NotificationType.CHAT, true);
        NotificationOption disabledOption = optionForUser(disabledId, NotificationType.CHAT, false);
        when(notificationOptionRepository.findByUserIdIn(any())).thenReturn(List.of(enabledOption, disabledOption));
        when(fcmTokenRepository.findFcmTokenByCreatedByIn(any())).thenReturn(List.of());

        fcmService.sendChatNotification(
                List.of(enabledId, disabledId), "제목", "내용", NotificationType.CHAT, "/url", 5L, "collapse");

        verify(fcmTokenRepository).findFcmTokenByCreatedByIn(List.of(enabledId));
    }

    private NotificationOption optionWithEnabled(NotificationType type, boolean enabled) {
        NotificationOption option = org.mockito.Mockito.mock(NotificationOption.class);
        when(option.isEnabled(type)).thenReturn(enabled);
        return option;
    }

    private NotificationOption optionForUser(Long userId, NotificationType type, boolean enabled) {
        NotificationOption option = org.mockito.Mockito.mock(NotificationOption.class);
        when(option.isEnabled(type)).thenReturn(enabled);
        User user = org.mockito.Mockito.mock(User.class);
        when(user.getUserId()).thenReturn(userId);
        when(option.getUser()).thenReturn(user);
        return option;
    }
}
