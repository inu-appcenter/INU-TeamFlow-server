package com.inuteamflow.server.global.websocket;

import java.security.Principal;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
public class WebSocketSessionEventLogger {

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = accessor(event.getMessage());
        Map<String, Object> attributes = sessionAttributes(accessor);

        log.info(
                "[WS 디버그] STOMP CONNECT 수신 sessionId={} traceId={} acceptVersion={} heartBeat={} cfRay={} cfConnectingIp={} userAgent={} subProtocol={} extensions={}",
                sessionId(accessor),
                value(attributes, WebSocketHandshakeLoggingInterceptor.TRACE_ID_ATTRIBUTE),
                nativeHeader(accessor, "accept-version"),
                nativeHeader(accessor, "heart-beat"),
                value(attributes, WebSocketHandshakeLoggingInterceptor.CF_RAY_ATTRIBUTE),
                value(attributes, WebSocketHandshakeLoggingInterceptor.CF_CONNECTING_IP_ATTRIBUTE),
                value(attributes, WebSocketHandshakeLoggingInterceptor.USER_AGENT_ATTRIBUTE),
                value(attributes, WebSocketHandshakeLoggingInterceptor.SUB_PROTOCOL_ATTRIBUTE),
                value(attributes, WebSocketHandshakeLoggingInterceptor.EXTENSIONS_ATTRIBUTE));
    }

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = accessor(event.getMessage());
        Map<String, Object> attributes = sessionAttributes(accessor);
        log.info(
                "[WS 디버그] 세션 CONNECTED sessionId={} traceId={} user={} durationMs={} cfRay={} userAgent={}",
                sessionId(accessor),
                value(attributes, WebSocketHandshakeLoggingInterceptor.TRACE_ID_ATTRIBUTE),
                principalName(event.getUser()),
                durationMillis(attributes),
                value(attributes, WebSocketHandshakeLoggingInterceptor.CF_RAY_ATTRIBUTE),
                value(attributes, WebSocketHandshakeLoggingInterceptor.USER_AGENT_ATTRIBUTE));
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = accessor(event.getMessage());
        Map<String, Object> attributes = sessionAttributes(accessor);
        log.info(
                "[WS 디버그] 세션 종료 sessionId={} traceId={} closeStatus={} user={} durationMs={} cfRay={} cfConnectingIp={} userAgent={}",
                event.getSessionId(),
                value(attributes, WebSocketHandshakeLoggingInterceptor.TRACE_ID_ATTRIBUTE),
                event.getCloseStatus(),
                principalName(event.getUser()),
                durationMillis(attributes),
                value(attributes, WebSocketHandshakeLoggingInterceptor.CF_RAY_ATTRIBUTE),
                value(attributes, WebSocketHandshakeLoggingInterceptor.CF_CONNECTING_IP_ATTRIBUTE),
                value(attributes, WebSocketHandshakeLoggingInterceptor.USER_AGENT_ATTRIBUTE));
    }

    private static StompHeaderAccessor accessor(org.springframework.messaging.Message<byte[]> message) {
        return MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    }

    private static Map<String, Object> sessionAttributes(StompHeaderAccessor accessor) {
        return accessor == null ? null : accessor.getSessionAttributes();
    }

    private static String sessionId(StompHeaderAccessor accessor) {
        return accessor == null ? "-" : String.valueOf(accessor.getSessionId());
    }

    private static String nativeHeader(StompHeaderAccessor accessor, String name) {
        if (accessor == null) {
            return "-";
        }
        String value = accessor.getFirstNativeHeader(name);
        return value == null ? "-" : value;
    }

    private static String principalName(Principal principal) {
        return principal == null ? "anonymous" : principal.getName();
    }

    private static long durationMillis(Map<String, Object> attributes) {
        if (attributes == null) {
            return -1;
        }
        Object startedAt = attributes.get(WebSocketHandshakeLoggingInterceptor.HANDSHAKE_STARTED_AT_ATTRIBUTE);
        return startedAt instanceof Long startedAtNanos ? (System.nanoTime() - startedAtNanos) / 1_000_000 : -1;
    }

    private static String value(Map<String, Object> attributes, String key) {
        return attributes == null ? "-" : String.valueOf(attributes.getOrDefault(key, "-"));
    }
}
