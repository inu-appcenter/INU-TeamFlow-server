package com.inuteamflow.server.global.websocket;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StompOutboundFrameLoggingInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            accessor = StompHeaderAccessor.wrap(message);
        }

        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECTED.equals(command) || StompCommand.ERROR.equals(command)) {
            Map<String, Object> attributes = accessor.getSessionAttributes();
            log.info(
                    "[WS 디버그] STOMP 아웃바운드 frame command={} sessionId={} traceId={} payloadLength={}",
                    command,
                    accessor.getSessionId(),
                    value(attributes, WebSocketHandshakeLoggingInterceptor.TRACE_ID_ATTRIBUTE),
                    payloadLength(message.getPayload()));
        }
        return message;
    }

    private static int payloadLength(Object payload) {
        return payload instanceof byte[] bytes ? bytes.length : -1;
    }

    private static String value(Map<String, Object> attributes, String key) {
        return attributes == null ? "-" : String.valueOf(attributes.getOrDefault(key, "-"));
    }
}
