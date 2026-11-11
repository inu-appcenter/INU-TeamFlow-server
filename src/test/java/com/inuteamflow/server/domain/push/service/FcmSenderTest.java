package com.inuteamflow.server.domain.push.service;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.inuteamflow.server.domain.push.dto.PushMessage;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link FcmSender}의 FCM 멀티캐스트 메시지 생성을 검증한다.
 * - 실제 발송은 Firebase 초기화가 필요하므로 메시지 생성 단계만 검증한다.
 */
class FcmSenderTest {

    private final FcmSender fcmSender = new FcmSender();

    @Test
    @DisplayName("축약 키가 있는 메시지도 예외 없이 생성한다")
    void toMulticast_withCollapseKey() {
        PushMessage message =
                new PushMessage("발신자", "안녕", Map.of("redirectUrl", "/chat/5", "type", "CHAT"), "chat-room-5");

        assertThatCode(() -> fcmSender.toMulticast(List.of("token"), message)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("축약 키가 없는 메시지를 예외 없이 생성한다")
    void toMulticast_withoutCollapseKey() {
        PushMessage message = new PushMessage("공지", "내용", Map.of("type", "NOTICE"), null);

        assertThatCode(() -> fcmSender.toMulticast(List.of("token"), message)).doesNotThrowAnyException();
    }
}
