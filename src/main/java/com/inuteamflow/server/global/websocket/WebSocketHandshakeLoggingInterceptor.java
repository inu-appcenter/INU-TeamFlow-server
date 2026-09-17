package com.inuteamflow.server.global.websocket;

import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Slf4j
@Component
public class WebSocketHandshakeLoggingInterceptor implements HandshakeInterceptor {

    static final String TRACE_ID_ATTRIBUTE = "wsTraceId";
    static final String HANDSHAKE_STARTED_AT_ATTRIBUTE = "wsHandshakeStartedAtNanos";
    static final String CF_RAY_ATTRIBUTE = "wsCfRay";
    static final String CF_CONNECTING_IP_ATTRIBUTE = "wsCfConnectingIp";
    static final String USER_AGENT_ATTRIBUTE = "wsUserAgent";
    static final String SUB_PROTOCOL_ATTRIBUTE = "wsSubProtocol";
    static final String EXTENSIONS_ATTRIBUTE = "wsExtensions";
    static final String STOMP_CONNECT_RECEIVED_ATTRIBUTE = "wsStompConnectReceived";
    static final String RAW_INBOUND_MESSAGE_COUNT_ATTRIBUTE = "wsRawInboundMessageCount";

    private static final int MAX_HEADER_LENGTH = 256;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        HttpHeaders headers = request.getHeaders();
        String traceId = UUID.randomUUID().toString();

        attributes.put(TRACE_ID_ATTRIBUTE, traceId);
        attributes.put(HANDSHAKE_STARTED_AT_ATTRIBUTE, System.nanoTime());
        putIfPresent(attributes, CF_RAY_ATTRIBUTE, safeHeader(headers.getFirst("CF-Ray")));
        putIfPresent(attributes, CF_CONNECTING_IP_ATTRIBUTE, safeHeader(headers.getFirst("CF-Connecting-IP")));
        putIfPresent(attributes, USER_AGENT_ATTRIBUTE, safeHeader(headers.getFirst(HttpHeaders.USER_AGENT)));
        putIfPresent(attributes, SUB_PROTOCOL_ATTRIBUTE, safeHeader(headers.getFirst("Sec-WebSocket-Protocol")));
        putIfPresent(attributes, EXTENSIONS_ATTRIBUTE, safeHeader(headers.getFirst("Sec-WebSocket-Extensions")));

        log.info(
                "[WS 디버그] 핸드셰이크 요청 traceId={} path={} remoteAddress={} cfRay={} cfConnectingIp={} userAgent={} subProtocol={} extensions={}",
                traceId,
                request.getURI().getPath(),
                request.getRemoteAddress(),
                value(attributes, CF_RAY_ATTRIBUTE),
                value(attributes, CF_CONNECTING_IP_ATTRIBUTE),
                value(attributes, USER_AGENT_ATTRIBUTE),
                value(attributes, SUB_PROTOCOL_ATTRIBUTE),
                value(attributes, EXTENSIONS_ATTRIBUTE));
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.warn(
                    "[WS 디버그] 핸드셰이크 실패 path={} remoteAddress={} exceptionType={} message={}",
                    request.getURI().getPath(),
                    request.getRemoteAddress(),
                    exception.getClass().getName(),
                    exception.getMessage(),
                    exception);
        }
    }

    private static void putIfPresent(Map<String, Object> attributes, String key, String value) {
        if (value != null) {
            attributes.put(key, value);
        }
    }

    private static String value(Map<String, Object> attributes, String key) {
        return String.valueOf(attributes.getOrDefault(key, "-"));
    }

    private static String safeHeader(String value) {
        if (value == null) {
            return null;
        }

        String sanitized = value.replace('\r', ' ').replace('\n', ' ');
        return sanitized.length() <= MAX_HEADER_LENGTH ? sanitized : sanitized.substring(0, MAX_HEADER_LENGTH);
    }
}
