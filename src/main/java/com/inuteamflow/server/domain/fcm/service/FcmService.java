package com.inuteamflow.server.domain.fcm.service;

import com.google.firebase.messaging.*;
import com.inuteamflow.server.domain.fcm.dto.req.FcmRequest;
import com.inuteamflow.server.domain.fcm.dto.res.FcmResponse;
import com.inuteamflow.server.domain.fcm.entity.FcmToken;
import com.inuteamflow.server.domain.fcm.repository.FcmTokenRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FcmService {

    private final FcmTokenRepository fcmTokenRepository;

    @Transactional
    public FcmResponse createFcmToken(
            User user,
            FcmRequest request
    ) {
        // 없을 때만 저장, 있으면 기존에 저장되어 있던 토큰을 반환
        FcmToken fcmToken = fcmTokenRepository.findByCreatedByAndFcmToken(user.getUserId(), request.getToken())
                .orElseGet(() -> fcmTokenRepository.save(FcmToken.create(request)));
        return FcmResponse.create(fcmToken);
    }

    @Transactional
    public void deleteFcmToken(
            User user,
            FcmRequest request
    ) {
        // 토큰이 존재하는 지 확인하고 삭제
        FcmToken fcmToken = fcmTokenRepository.findByCreatedByAndFcmToken(user.getUserId(), request.getToken())
                .orElseThrow(() -> new RestApiException(CustomErrorCode.FCM_TOKEN_NOT_FOUND));
        fcmTokenRepository.delete(fcmToken);
    }

    @Transactional
    public void sendToUser(User receiver, String title, String body) {
        List<String> tokens = fcmTokenRepository.findFcmTokenByCreatedBy(receiver.getUserId());
        if (tokens.isEmpty()) return;

        MulticastMessage message = MulticastMessage.builder()
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .addAllTokens(tokens)
                .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            log.info("FCM 발송 완료 - userId: {}, 성공: {}/{}", receiver.getUserId(), response.getSuccessCount(), tokens.size());
            removeInvalidTokens(tokens, response);
        } catch (FirebaseMessagingException e) {
            log.error("FCM 발송 실패 - userId: {}", receiver.getUserId(), e);
        }
    }

    @Transactional
    public void sendToUsers(List<User> receivers, String title, String body) {
        List<Long> userIds = receivers.stream().map(User::getUserId).collect(Collectors.toList());
        List<String> tokens = fcmTokenRepository.findFcmTokenByCreatedByIn(userIds);
        if (tokens.isEmpty()) return;

        MulticastMessage message = MulticastMessage.builder()
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .addAllTokens(tokens)
                .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            log.info("FCM 다중 발송 완료 - userIds: {}, 성공: {}/{}", userIds, response.getSuccessCount(), tokens.size());
            removeInvalidTokens(tokens, response);
        } catch (FirebaseMessagingException e) {
            log.error("FCM 다중 발송 실패", e);
        }
    }

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
