package com.inuteamflow.server.domain.push.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.inuteamflow.server.domain.push.dto.PushMessage;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * {@link ExpoSender}의 Expo Push API 요청 형식과 응답 처리를 검증한다.
 * - 실제 Expo 서버 대신 {@link MockRestServiceServer}로 응답을 흉내 낸다.
 */
class ExpoSenderTest {

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";
    private static final PushMessage MESSAGE =
            new PushMessage("발신자", "안녕", Map.of("redirectUrl", "/chat/5", "type", "CHAT"), "chat-room-5");

    private MockRestServiceServer server;
    private ExpoSender expoSender;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://exp.host/--/api/v2/push");
        server = MockRestServiceServer.bindTo(builder).build();
        expoSender = new ExpoSender(builder.build());
    }

    @Test
    @DisplayName("토큰마다 메시지 1개씩 Expo 형식(to, title, body, data, sound, collapseId)으로 요청한다")
    void send_requestsExpoFormat() {
        server.expect(requestTo(EXPO_PUSH_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        [
                          {"to": "ExponentPushToken[a]", "title": "발신자", "body": "안녕",
                           "data": {"redirectUrl": "/chat/5", "type": "CHAT"},
                           "sound": "default", "collapseId": "chat-room-5"},
                          {"to": "ExponentPushToken[b]"}
                        ]
                        """))
                .andRespond(withSuccess("""
                        {"data": [{"status": "ok", "id": "1"}, {"status": "ok", "id": "2"}]}
                        """, MediaType.APPLICATION_JSON));

        List<String> invalidTokens = expoSender.send(List.of("ExponentPushToken[a]", "ExponentPushToken[b]"), MESSAGE);

        assertThat(invalidTokens).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("축약 키가 없으면 collapseId를 요청에 포함하지 않는다")
    void send_omitsCollapseIdWhenNull() {
        server.expect(requestTo(EXPO_PUSH_URL))
                .andExpect(request -> assertThat(((MockClientHttpRequest) request).getBodyAsString())
                        .doesNotContain("collapseId"))
                .andRespond(withSuccess("{\"data\": [{\"status\": \"ok\"}]}", MediaType.APPLICATION_JSON));

        expoSender.send(List.of("ExponentPushToken[a]"), new PushMessage("제목", "내용", Map.of(), null));

        server.verify();
    }

    @Test
    @DisplayName("DeviceNotRegistered ticket을 받은 토큰만 무효 토큰으로 반환한다")
    void send_returnsDeviceNotRegisteredTokens() {
        server.expect(requestTo(EXPO_PUSH_URL)).andRespond(withSuccess("""
                        {"data": [
                          {"status": "ok", "id": "1"},
                          {"status": "error", "message": "not registered", "details": {"error": "DeviceNotRegistered"}},
                          {"status": "error", "message": "too big", "details": {"error": "MessageTooBig"}}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        List<String> invalidTokens = expoSender.send(
                List.of("ExponentPushToken[ok]", "ExponentPushToken[gone]", "ExponentPushToken[big]"), MESSAGE);

        assertThat(invalidTokens).containsExactly("ExponentPushToken[gone]");
    }

    @Test
    @DisplayName("토큰이 100개를 넘으면 요청을 100개 단위로 나누어 보낸다")
    void send_splitsIntoBatchesOf100() {
        List<String> tokens = IntStream.range(0, 150)
                .mapToObj(i -> "ExponentPushToken[" + i + "]")
                .toList();
        server.expect(times(2), requestTo(EXPO_PUSH_URL))
                .andRespond(withSuccess("{\"data\": []}", MediaType.APPLICATION_JSON));

        expoSender.send(tokens, MESSAGE);

        server.verify();
    }

    @Test
    @DisplayName("Expo 서버 오류가 나도 예외를 전파하지 않고 빈 목록을 반환한다")
    void send_swallowsServerError() {
        server.expect(requestTo(EXPO_PUSH_URL)).andRespond(withServerError());

        List<String> invalidTokens = expoSender.send(List.of("ExponentPushToken[a]"), MESSAGE);

        assertThat(invalidTokens).isEmpty();
    }

    @Test
    @DisplayName("요청 단위 오류(errors)만 있고 ticket이 없으면 빈 목록을 반환한다")
    void send_handlesRequestLevelErrors() {
        server.expect(requestTo(EXPO_PUSH_URL)).andRespond(withSuccess("""
                        {"errors": [{"code": "PUSH_TOO_MANY_EXPERIENCE_IDS", "message": "mixed projects"}]}
                        """, MediaType.APPLICATION_JSON));

        List<String> invalidTokens = expoSender.send(List.of("ExponentPushToken[a]"), MESSAGE);

        assertThat(invalidTokens).isEmpty();
    }
}
