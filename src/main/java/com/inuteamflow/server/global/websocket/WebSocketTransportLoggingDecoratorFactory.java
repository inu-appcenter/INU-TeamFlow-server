package com.inuteamflow.server.global.websocket;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

@Slf4j
@Component
public class WebSocketTransportLoggingDecoratorFactory implements WebSocketHandlerDecoratorFactory {

    @Override
    public WebSocketHandler decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {
            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                Map<String, Object> attributes = session.getAttributes();
                log.warn(
                        "[WS 디버그] 전송 계층 오류 sessionId={} traceId={} durationMs={} cfRay={} cfConnectingIp={} userAgent={} exceptionType={} message={}",
                        session.getId(),
                        value(attributes, WebSocketHandshakeLoggingInterceptor.TRACE_ID_ATTRIBUTE),
                        durationMillis(attributes),
                        value(attributes, WebSocketHandshakeLoggingInterceptor.CF_RAY_ATTRIBUTE),
                        value(attributes, WebSocketHandshakeLoggingInterceptor.CF_CONNECTING_IP_ATTRIBUTE),
                        value(attributes, WebSocketHandshakeLoggingInterceptor.USER_AGENT_ATTRIBUTE),
                        exception.getClass().getName(),
                        exception.getMessage(),
                        exception);
                super.handleTransportError(session, exception);
            }
        };
    }

    private static long durationMillis(Map<String, Object> attributes) {
        Object startedAt = attributes.get(WebSocketHandshakeLoggingInterceptor.HANDSHAKE_STARTED_AT_ATTRIBUTE);
        return startedAt instanceof Long startedAtNanos ? (System.nanoTime() - startedAtNanos) / 1_000_000 : -1;
    }

    private static String value(Map<String, Object> attributes, String key) {
        return String.valueOf(attributes.getOrDefault(key, "-"));
    }
}
