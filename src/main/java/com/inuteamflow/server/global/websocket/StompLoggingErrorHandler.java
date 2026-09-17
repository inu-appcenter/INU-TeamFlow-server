package com.inuteamflow.server.global.websocket;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

@Slf4j
@Component
public class StompLoggingErrorHandler extends StompSubProtocolErrorHandler {

    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable exception) {
        StompHeaderAccessor accessor = clientMessage == null
                ? null
                : MessageHeaderAccessor.getAccessor(clientMessage, StompHeaderAccessor.class);
        Map<String, Object> attributes = accessor == null ? null : accessor.getSessionAttributes();

        log.warn(
                "[WS 디버그] STOMP 처리 오류 sessionId={} traceId={} command={} exceptionType={} message={}",
                accessor == null ? "-" : accessor.getSessionId(),
                value(attributes, WebSocketHandshakeLoggingInterceptor.TRACE_ID_ATTRIBUTE),
                accessor == null ? "-" : accessor.getCommand(),
                exception.getClass().getName(),
                exception.getMessage(),
                exception);

        return super.handleClientMessageProcessingError(clientMessage, exception);
    }

    private static String value(Map<String, Object> attributes, String key) {
        return attributes == null ? "-" : String.valueOf(attributes.getOrDefault(key, "-"));
    }
}
