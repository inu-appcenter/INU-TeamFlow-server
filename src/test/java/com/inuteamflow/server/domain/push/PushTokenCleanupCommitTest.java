package com.inuteamflow.server.domain.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.inuteamflow.server.domain.notification.enums.NotificationType;
import com.inuteamflow.server.domain.notification.service.NotificationService;
import com.inuteamflow.server.domain.push.entity.PushToken;
import com.inuteamflow.server.domain.push.enums.PushProvider;
import com.inuteamflow.server.domain.push.repository.PushTokenRepository;
import com.inuteamflow.server.domain.push.service.ExpoSender;
import com.inuteamflow.server.domain.push.service.FcmSender;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.domain.user.enums.Department;
import com.inuteamflow.server.domain.user.enums.Role;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 알림 생성 트랜잭션 커밋 이후(AFTER_COMMIT) 푸시 전용 스레드에서 실행되는 발송에서 무효 토큰 삭제가 실제로 커밋되는지 검증한다.
 * - 알림 생성, 결과 조회를 각각 독립된 트랜잭션으로 실행한다.
 * - 실제 Firebase, Expo 발송 대신 {@link FcmSender}, {@link ExpoSender}를 mock으로 대체한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class PushTokenCleanupCommitTest {

    private static final String INVALID_TOKEN = "unregistered-fcm-token";

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PushTokenRepository pushTokenRepository;

    @MockitoBean
    private FcmSender fcmSender;

    @MockitoBean
    private ExpoSender expoSender;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("발송 후 등록 해제된 것으로 응답한 토큰의 삭제가 실제 DB에 커밋된다")
    void sendAfterCommit_commitsInvalidTokenDeletion() {
        User receiver = transactionTemplate.execute(status -> {
            User user = createUser("push-cleanup", "push-cleanup@inu.ac.kr");
            actingAs(user);
            pushTokenRepository.save(PushToken.builder()
                    .token(INVALID_TOKEN)
                    .deviceType("web")
                    .provider(PushProvider.FCM)
                    .build());
            return user;
        });
        assertThat(receiver).isNotNull();
        AtomicReference<String> sendThreadName = new AtomicReference<>();
        when(fcmSender.send(any(), any())).thenAnswer(invocation -> {
            sendThreadName.set(Thread.currentThread().getName());
            return List.of(INVALID_TOKEN);
        });

        // 테스트 트랜잭션이 없는 상태에서 호출하므로 메서드 반환 전에 알림 저장이 커밋되고, 발송은 푸시 전용 스레드로 넘어간다.
        notificationService.createNotification(receiver, "제목", "내용", NotificationType.CALENDAR, "/calendar");

        verify(fcmSender, timeout(2000)).send(eq(List.of(INVALID_TOKEN)), any());
        assertThat(sendThreadName.get()).startsWith("push-");
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> transactionTemplate.executeWithoutResult(status -> assertThat(
                                pushTokenRepository.findByCreatedByAndToken(receiver.getUserId(), INVALID_TOKEN))
                        .isEmpty()));
    }

    private User createUser(String username, String email) {
        return userRepository.save(User.builder()
                .username(username)
                .email(email)
                .password("encoded-password")
                .name(username)
                .department(Department.COMPUTER_SCIENCE)
                .studentNumber(null)
                .isSchoolVerified(false)
                .role(Role.USER)
                .imageKey(null)
                .build());
    }

    private void actingAs(User user) {
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }
}
