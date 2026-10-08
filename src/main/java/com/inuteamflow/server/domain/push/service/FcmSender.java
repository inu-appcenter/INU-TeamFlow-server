package com.inuteamflow.server.domain.push.service;

import com.google.firebase.messaging.*;
import com.inuteamflow.server.domain.push.dto.PushMessage;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FcmSender {

    private static final int MAX_BATCH_SIZE = 500;

    /**
     * FCM 토큰으로 Firebase Admin SDK를 통해 푸시 알림을 발송한다.
     *
     * <p>Firebase 멀티캐스트 제한에 맞춰 토큰을 최대 {@value #MAX_BATCH_SIZE}개씩 나누어 발송한다.
     * 축약 키가 있으면 Android와 iOS에 동일하게 설정하여 같은 키의 미확인 알림이 중복 표시되지 않도록 한다.
     * Firebase 발송 실패는 기록하고 호출자에게 전파하지 않는다.</p>
     *
     * @param tokens 발송할 FCM 토큰 목록
     * @param message 발송할 알림 내용
     * @return Firebase가 등록 해제된 것으로 응답한 토큰 목록
     */
    public List<String> send(List<String> tokens, PushMessage message) {
        List<String> invalidTokens = new ArrayList<>();
        for (List<String> batch : TokenBatches.partition(tokens, MAX_BATCH_SIZE)) {
            try {
                BatchResponse response =
                        FirebaseMessaging.getInstance().sendEachForMulticast(toMulticast(batch, message));
                log.info("FCM 발송 완료 - 성공: {}/{}", response.getSuccessCount(), batch.size());
                invalidTokens.addAll(findUnregisteredTokens(batch, response));
            } catch (FirebaseMessagingException e) {
                log.error("FCM 발송 실패", e);
            }
        }
        return invalidTokens;
    }

    private MulticastMessage toMulticast(List<String> batch, PushMessage message) {
        MulticastMessage.Builder builder = MulticastMessage.builder()
                .setNotification(Notification.builder()
                        .setTitle(message.title())
                        .setBody(message.body())
                        .build())
                .putAllData(message.data())
                .addAllTokens(batch);

        if (message.collapseKey() != null) {
            builder.setAndroidConfig(AndroidConfig.builder()
                            .setCollapseKey(message.collapseKey())
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .putHeader("apns-collapse-id", message.collapseKey())
                            .build());
        }
        return builder.build();
    }

    /**
     * Firebase에서 등록 해제된 것으로 응답한 토큰을 찾는다.
     *
     * <p>배치 응답과 요청 토큰의 인덱스를 대응시켜
     * {@link MessagingErrorCode#UNREGISTERED} 오류가 발생한 토큰만 반환한다.</p>
     *
     * @param tokens 배치 발송에 사용한 FCM 토큰 목록
     * @param response Firebase의 배치 발송 응답
     * @return 등록 해제된 토큰 목록
     */
    private List<String> findUnregisteredTokens(List<String> tokens, BatchResponse response) {
        List<String> invalidTokens = new ArrayList<>();
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (!sendResponse.isSuccessful()) {
                FirebaseMessagingException e = sendResponse.getException();
                log.warn(
                        "FCM 발송 실패 - error: {}, message: {}",
                        e != null ? e.getMessagingErrorCode() : null,
                        e != null ? e.getMessage() : null);
                if (e != null && MessagingErrorCode.UNREGISTERED.equals(e.getMessagingErrorCode())) {
                    invalidTokens.add(tokens.get(i));
                }
            }
        }
        return invalidTokens;
    }
}
