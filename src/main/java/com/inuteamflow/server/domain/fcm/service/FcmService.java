package com.inuteamflow.server.domain.fcm.service;

import com.google.firebase.messaging.*;
import com.inuteamflow.server.domain.fcm.dto.req.FcmRequest;
import com.inuteamflow.server.domain.fcm.dto.res.FcmResponse;
import com.inuteamflow.server.domain.fcm.entity.FcmToken;
import com.inuteamflow.server.domain.fcm.repository.FcmTokenRepository;
import com.inuteamflow.server.domain.notification.enums.NotificationType;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FcmService {

    private final FcmTokenRepository fcmTokenRepository;

    private static final int FCM_MAX_BATCH_SIZE = 500;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 사용자의 FCM 토큰을 등록한다.
     *
     * <p>동일한 사용자가 같은 토큰을 이미 등록한 경우 새로 저장하지 않고 기존 토큰 정보를 반환한다.</p>
     *
     * @param user 토큰을 등록하는 사용자
     * @param request 등록할 FCM 토큰 요청
     * @return 등록되었거나 기존에 존재하는 FCM 토큰 정보
     */
    @Transactional
    public FcmResponse createFcmToken(User user, FcmRequest request) {
        FcmToken fcmToken = fcmTokenRepository
                .findByCreatedByAndFcmToken(user.getUserId(), request.getToken())
                .orElseGet(() -> fcmTokenRepository.save(FcmToken.create(request)));
        return FcmResponse.from(fcmToken);
    }

    /**
     * 사용자의 FCM 토큰을 삭제한다.
     *
     * <p>요청 사용자에게 등록된 토큰만 삭제할 수 있다.</p>
     *
     * @param user 토큰을 삭제하는 사용자
     * @param request 삭제할 FCM 토큰 요청
     * @throws RestApiException 사용자에게 등록된 토큰을 찾을 수 없는 경우
     */
    @Transactional
    public void deleteFcmToken(User user, FcmRequest request) {
        FcmToken fcmToken = fcmTokenRepository
                .findByCreatedByAndFcmToken(user.getUserId(), request.getToken())
                .orElseThrow(() -> new RestApiException(CustomErrorCode.FCM_TOKEN_NOT_FOUND));
        fcmTokenRepository.delete(fcmToken);
    }

    /**
     * 단일 사용자에게 FCM 알림을 발송한다.
     *
     * <p>등록된 토큰이 없으면 발송하지 않으며, 발송 후 Firebase에서 등록 해제된 것으로 응답한 토큰을 삭제한다.
     * Firebase 발송 실패는 기록하고 호출자에게 전파하지 않는다.</p>
     *
     * @param receiverId 알림 수신자 ID
     * @param title 알림 제목
     * @param body 알림 본문
     * @param redirectUrl 알림 선택 시 이동할 URL
     * @param type 알림 유형
     * @param notificationId 알림 ID
     */
    @Transactional
    public void sendToUser(
            Long receiverId,
            String title,
            String body,
            String redirectUrl,
            NotificationType type,
            Long notificationId) {
        List<String> tokens = fcmTokenRepository.findFcmTokenByCreatedBy(receiverId);
        if (tokens.isEmpty()) return;

        MulticastMessage message = MulticastMessage.builder()
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putData("redirectUrl", redirectUrl)
                .putData("type", type.name())
                .putData("notificationId", String.valueOf(notificationId))
                .addAllTokens(tokens)
                .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            log.info("FCM 발송 완료 - userId: {}, 성공: {}/{}", receiverId, response.getSuccessCount(), tokens.size());
            removeInvalidTokens(tokens, response);
        } catch (FirebaseMessagingException e) {
            log.error("FCM 발송 실패 - userId: {}", receiverId, e);
        }
    }

    /**
     * 여러 사용자에게 FCM 알림을 발송한다.
     *
     * <p>Firebase 멀티캐스트 제한에 맞춰 토큰을 최대 {@value #FCM_MAX_BATCH_SIZE}개씩 나누어 발송하고,
     * 각 배치에서 등록 해제된 토큰을 삭제한다.</p>
     *
     * @param receiverIds 알림 수신자 ID 목록
     * @param title 알림 제목
     * @param body 알림 본문
     * @param redirectUrl 알림 선택 시 이동할 URL
     * @param type 알림 유형
     */
    @Transactional
    public void sendToUsers(
            List<Long> receiverIds, String title, String body, String redirectUrl, NotificationType type) {
        List<String> tokens = fcmTokenRepository.findFcmTokenByCreatedByIn(receiverIds);
        if (tokens.isEmpty()) return;

        for (List<String> batch : partitionTokens(tokens)) {
            MulticastMessage message = MulticastMessage.builder()
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("redirectUrl", redirectUrl)
                    .putData("type", type.name())
                    .addAllTokens(batch)
                    .build();

            try {
                BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
                log.info(
                        "FCM 다중 발송 완료 - userIds: {}, 성공: {}/{}", receiverIds, response.getSuccessCount(), batch.size());
                removeInvalidTokens(batch, response);
            } catch (FirebaseMessagingException e) {
                log.error("FCM 다중 발송 실패", e);
            }
        }
    }

    /**
     * 여러 사용자에게 채팅 FCM 알림을 발송한다.
     *
     * <p>Android와 iOS에 동일한 축약 키를 설정하여 같은 채팅방의 미확인 알림이 중복 표시되지 않도록 하고,
     * 토큰은 최대 {@value #FCM_MAX_BATCH_SIZE}개씩 나누어 발송한다.</p>
     *
     * @param receiverIds 알림 수신자 ID 목록
     * @param title 알림 제목
     * @param body 알림 본문
     * @param type 알림 유형
     * @param redirectUrl 알림 선택 시 이동할 URL
     * @param roomId 채팅방 ID
     * @param collapseKey 동일 채팅방 알림을 식별하는 축약 키
     */
    @Transactional
    public void sendChatNotification(
            List<Long> receiverIds,
            String title,
            String body,
            NotificationType type,
            String redirectUrl,
            Long roomId,
            String collapseKey) {
        List<String> tokens = fcmTokenRepository.findFcmTokenByCreatedByIn(receiverIds);
        if (tokens.isEmpty()) return;

        for (List<String> batch : partitionTokens(tokens)) {
            MulticastMessage message = MulticastMessage.builder()
                    .setNotification(
                            Notification.builder().setTitle(title).setBody(body).build())
                    .setAndroidConfig(
                            AndroidConfig.builder().setCollapseKey(collapseKey).build())
                    .setApnsConfig(ApnsConfig.builder()
                            .putHeader("apns-collapse-id", collapseKey)
                            .build())
                    .putData("redirectUrl", redirectUrl)
                    .putData("type", type.name())
                    .putData("roomId", String.valueOf(roomId))
                    .addAllTokens(batch)
                    .build();

            try {
                BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
                log.info("FCM 채팅 알림 발송 완료 - 성공: {}/{}", response.getSuccessCount(), batch.size());
                removeInvalidTokens(batch, response);
            } catch (FirebaseMessagingException e) {
                log.error("FCM 채팅 알림 발송 실패", e);
            }
        }
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * FCM 토큰을 Firebase 멀티캐스트 제한 크기로 분할한다.
     *
     * <p>마지막 배치는 남은 토큰 수만 포함하며 원본 목록의 순서를 유지한다.</p>
     *
     * @param tokens 분할할 FCM 토큰 목록
     * @return 최대 {@value #FCM_MAX_BATCH_SIZE}개 단위로 분할된 토큰 목록
     */
    private List<List<String>> partitionTokens(List<String> tokens) {
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i += FCM_MAX_BATCH_SIZE) {
            batches.add(tokens.subList(i, Math.min(i + FCM_MAX_BATCH_SIZE, tokens.size())));
        }
        return batches;
    }

    /**
     * Firebase에서 등록 해제된 것으로 응답한 FCM 토큰을 삭제한다.
     *
     * <p>배치 응답과 요청 토큰의 인덱스를 대응시켜
     * {@link MessagingErrorCode#UNREGISTERED} 오류가 발생한 토큰만 제거한다.</p>
     *
     * @param tokens 배치 발송에 사용한 FCM 토큰 목록
     * @param response Firebase의 배치 발송 응답
     */
    private void removeInvalidTokens(List<String> tokens, BatchResponse response) {
        List<String> invalidTokens = new ArrayList<>();
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (!sendResponse.isSuccessful()) {
                FirebaseMessagingException e = sendResponse.getException();
                if (e != null && MessagingErrorCode.UNREGISTERED.equals(e.getMessagingErrorCode())) {
                    invalidTokens.add(tokens.get(i));
                }
            }
        }
        if (!invalidTokens.isEmpty()) {
            fcmTokenRepository.deleteByFcmTokenIn(invalidTokens);
        }
    }
}
