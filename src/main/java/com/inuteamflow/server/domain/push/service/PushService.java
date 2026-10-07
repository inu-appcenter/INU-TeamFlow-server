package com.inuteamflow.server.domain.push.service;

import com.inuteamflow.server.domain.notification.enums.NotificationType;
import com.inuteamflow.server.domain.notification.repository.NotificationOptionRepository;
import com.inuteamflow.server.domain.push.dto.PushMessage;
import com.inuteamflow.server.domain.push.dto.req.PushTokenRequest;
import com.inuteamflow.server.domain.push.dto.res.PushTokenResponse;
import com.inuteamflow.server.domain.push.entity.PushToken;
import com.inuteamflow.server.domain.push.enums.PushProvider;
import com.inuteamflow.server.domain.push.repository.PushTokenRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PushService {

    private final PushTokenRepository pushTokenRepository;
    private final NotificationOptionRepository notificationOptionRepository;
    private final FcmSender fcmSender;
    private final ExpoSender expoSender;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 사용자의 푸시 토큰을 등록한다.
     *
     * <p>동일한 사용자가 같은 토큰을 이미 등록한 경우 새로 저장하지 않고 기존 토큰 정보를 반환한다.</p>
     *
     * @param user 토큰을 등록하는 사용자
     * @param request 등록할 푸시 토큰 요청
     * @return 등록되었거나 기존에 존재하는 푸시 토큰 정보
     */
    @Transactional
    public PushTokenResponse createPushToken(User user, PushTokenRequest request) {
        PushToken pushToken = pushTokenRepository
                .findByCreatedByAndToken(user.getUserId(), request.getToken())
                .orElseGet(() -> pushTokenRepository.save(PushToken.create(request)));
        return PushTokenResponse.from(pushToken);
    }

    /**
     * 사용자의 푸시 토큰을 삭제한다.
     *
     * <p>요청 사용자에게 등록된 토큰만 삭제할 수 있다.</p>
     *
     * @param user 토큰을 삭제하는 사용자
     * @param request 삭제할 푸시 토큰 요청
     * @throws RestApiException 사용자에게 등록된 토큰을 찾을 수 없는 경우
     */
    @Transactional
    public void deletePushToken(User user, PushTokenRequest request) {
        PushToken pushToken = pushTokenRepository
                .findByCreatedByAndToken(user.getUserId(), request.getToken())
                .orElseThrow(() -> new RestApiException(CustomErrorCode.PUSH_TOKEN_NOT_FOUND));
        pushTokenRepository.delete(pushToken);
    }

    /**
     * 단일 사용자에게 푸시 알림을 발송한다.
     *
     * <p>수신자의 알림 옵션에서 해당 유형이 비활성화되어 있으면 발송하지 않는다.
     * 등록된 토큰을 provider별로 나누어 FCM 토큰은 Firebase로, Expo 토큰은 Expo Push API로 발송하고,
     * 발송 후 등록 해제된 것으로 응답한 토큰을 삭제한다.</p>
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
        boolean enabled = notificationOptionRepository
                .findByUserId(receiverId)
                .map(option -> option.isEnabled(type))
                .orElse(true);
        if (!enabled) return;

        Map<String, String> data = new HashMap<>();
        data.put("redirectUrl", redirectUrl);
        data.put("type", type.name());
        data.put("notificationId", String.valueOf(notificationId));

        send(pushTokenRepository.findAllByCreatedBy(receiverId), new PushMessage(title, body, data, null));
    }

    /**
     * 여러 사용자에게 푸시 알림을 발송한다.
     *
     * <p>알림 옵션에서 해당 유형이 비활성화된 수신자는 발송 대상에서 제외한다.
     * 등록된 토큰을 provider별로 나누어 발송하고, 등록 해제된 것으로 응답한 토큰을 삭제한다.</p>
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
        receiverIds = filterEnabledReceivers(receiverIds, type);
        if (receiverIds.isEmpty()) return;

        Map<String, String> data = new HashMap<>();
        data.put("redirectUrl", redirectUrl);
        data.put("type", type.name());

        send(pushTokenRepository.findAllByCreatedByIn(receiverIds), new PushMessage(title, body, data, null));
    }

    /**
     * 여러 사용자에게 채팅 푸시 알림을 발송한다.
     *
     * <p>알림 옵션에서 해당 유형이 비활성화된 수신자는 발송 대상에서 제외한다.
     * 같은 채팅방의 미확인 알림이 중복 표시되지 않도록 축약 키를 함께 발송한다.</p>
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
        receiverIds = filterEnabledReceivers(receiverIds, type);
        if (receiverIds.isEmpty()) return;

        Map<String, String> data = new HashMap<>();
        data.put("redirectUrl", redirectUrl);
        data.put("type", type.name());
        data.put("roomId", String.valueOf(roomId));

        send(pushTokenRepository.findAllByCreatedByIn(receiverIds), new PushMessage(title, body, data, collapseKey));
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 토큰을 provider별로 나누어 해당 채널로 발송한다.
     *
     * <p>FCM 토큰은 {@link FcmSender}, Expo 토큰은 {@link ExpoSender}로 발송하고,
     * 각 채널에서 등록 해제된 것으로 응답한 토큰을 모아 한 번에 삭제한다.</p>
     *
     * @param tokens 발송 대상 푸시 토큰 목록
     * @param message 발송할 알림 내용
     */
    private void send(List<PushToken> tokens, PushMessage message) {
        if (tokens.isEmpty()) return;

        Map<PushProvider, List<String>> tokensByProvider = tokens.stream()
                .collect(Collectors.groupingBy(
                        PushToken::getProvider,
                        () -> new EnumMap<>(PushProvider.class),
                        Collectors.mapping(PushToken::getToken, Collectors.toList())));

        List<String> invalidTokens = new ArrayList<>();
        List<String> fcmTokens = tokensByProvider.getOrDefault(PushProvider.FCM, List.of());
        if (!fcmTokens.isEmpty()) {
            invalidTokens.addAll(fcmSender.send(fcmTokens, message));
        }
        List<String> expoTokens = tokensByProvider.getOrDefault(PushProvider.EXPO, List.of());
        if (!expoTokens.isEmpty()) {
            invalidTokens.addAll(expoSender.send(expoTokens, message));
        }

        if (!invalidTokens.isEmpty()) {
            pushTokenRepository.deleteByTokenIn(invalidTokens);
        }
    }

    /**
     * 알림 옵션에서 해당 유형이 활성화된 수신자만 남긴다.
     *
     * <p>옵션이 비활성화된 수신자를 제외하며, 옵션이 없는 수신자는 발송 대상으로 간주한다.</p>
     *
     * @param receiverIds 알림 수신자 ID 목록
     * @param type 알림 유형
     * @return 해당 유형 알림을 수신할 수신자 ID 목록
     */
    private List<Long> filterEnabledReceivers(List<Long> receiverIds, NotificationType type) {
        Set<Long> disabled = notificationOptionRepository.findByUserIdIn(receiverIds).stream()
                .filter(option -> !option.isEnabled(type))
                .map(option -> option.getUser().getUserId())
                .collect(Collectors.toSet());
        return receiverIds.stream().filter(id -> !disabled.contains(id)).toList();
    }
}
