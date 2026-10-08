package com.inuteamflow.server.domain.push.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.inuteamflow.server.domain.push.dto.PushMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpoSender {

    private static final int MAX_BATCH_SIZE = 100;
    private static final String DEVICE_NOT_REGISTERED = "DeviceNotRegistered";

    private final RestClient expoRestClient;

    /**
     * Expo Push Token으로 Expo Push API를 통해 푸시 알림을 발송한다.
     *
     * <p>Expo Push API 제한에 맞춰 토큰을 최대 {@value #MAX_BATCH_SIZE}개씩 나누어 발송한다.
     * 응답 ticket과 토큰을 순서대로 대응시키기 위해 메시지 1개에 토큰 1개만 담는다.
     * Expo 발송 실패는 기록하고 호출자에게 전파하지 않는다.</p>
     *
     * @param tokens 발송할 Expo Push Token 목록
     * @param message 발송할 알림 내용
     * @return Expo가 {@value #DEVICE_NOT_REGISTERED}로 응답한 토큰 목록
     */
    public List<String> send(List<String> tokens, PushMessage message) {
        List<String> invalidTokens = new ArrayList<>();
        for (List<String> batch : TokenBatches.partition(tokens, MAX_BATCH_SIZE)) {
            List<ExpoPushMessage> messages = batch.stream()
                    .map(token -> ExpoPushMessage.of(token, message))
                    .toList();
            try {
                ExpoPushResponse response = expoRestClient
                        .post()
                        .uri("/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(messages)
                        .retrieve()
                        .body(ExpoPushResponse.class);
                invalidTokens.addAll(findUnregisteredTokens(batch, response));
            } catch (RestClientException e) {
                log.error("Expo 발송 실패", e);
            }
        }
        return invalidTokens;
    }

    /**
     * Expo에서 등록 해제된 것으로 응답한 토큰을 찾는다.
     *
     * <p>HTTP 200 응답이어도 요청 단위 오류(errors)가 올 수 있어 기록한다.
     * ticket과 요청 토큰의 인덱스를 대응시켜 {@value #DEVICE_NOT_REGISTERED} 오류가 발생한 토큰만 반환하고,
     * 그 외 ticket 오류는 기록만 한다.</p>
     *
     * @param tokens 배치 발송에 사용한 Expo Push Token 목록
     * @param response Expo Push API 응답
     * @return 등록 해제된 토큰 목록
     */
    private List<String> findUnregisteredTokens(List<String> tokens, ExpoPushResponse response) {
        if (response == null) return List.of();
        if (response.errors() != null && !response.errors().isEmpty()) {
            log.error("Expo 발송 요청 오류 - {}", response.errors());
        }
        if (response.data() == null) return List.of();

        List<String> invalidTokens = new ArrayList<>();
        int successCount = 0;
        for (int i = 0; i < response.data().size() && i < tokens.size(); i++) {
            Ticket ticket = response.data().get(i);
            if ("ok".equals(ticket.status())) {
                successCount++;
            } else if (DEVICE_NOT_REGISTERED.equals(ticket.errorCode())) {
                invalidTokens.add(tokens.get(i));
            } else {
                log.warn("Expo 발송 실패 - error: {}, message: {}", ticket.errorCode(), ticket.message());
            }
        }
        log.info("Expo 발송 완료 - 성공: {}/{}", successCount, tokens.size());
        return invalidTokens;
    }

    // Expo Push API 요청 메시지
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ExpoPushMessage(
            String to, String title, String body, Map<String, String> data, String sound, String collapseId) {

        static ExpoPushMessage of(String token, PushMessage message) {
            return new ExpoPushMessage(
                    token, message.title(), message.body(), message.data(), "default", message.collapseKey());
        }
    }

    // Expo Push API 응답 (data: 요청 메시지와 같은 순서의 ticket 목록, errors: 요청 단위 오류)
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ExpoPushResponse(List<Ticket> data, List<RequestError> errors) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Ticket(String status, String message, Map<String, Object> details) {

        String errorCode() {
            return details == null ? null : (String) details.get("error");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RequestError(String code, String message) {}
}
