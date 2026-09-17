package com.inuteamflow.server.global.websocket;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

@Slf4j
@Component
public class WebSocketTransportLoggingDecoratorFactory implements WebSocketHandlerDecoratorFactory {

    private static final int RAW_MESSAGE_LOG_LIMIT_BEFORE_CONNECT = 10;
    private static final byte[] CONNECT_COMMAND = "CONNECT".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] STOMP_COMMAND = "STOMP".getBytes(StandardCharsets.US_ASCII);

    @Override
    public WebSocketHandler decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {
            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                logInboundMessageMetadata(session, message);
                super.handleMessage(session, message);
            }

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

    private static void logInboundMessageMetadata(WebSocketSession session, WebSocketMessage<?> message) {
        Map<String, Object> attributes = session.getAttributes();
        if (Boolean.TRUE.equals(
                attributes.get(WebSocketHandshakeLoggingInterceptor.STOMP_CONNECT_RECEIVED_ATTRIBUTE))) {
            return;
        }

        AtomicInteger messageCount = (AtomicInteger) attributes.computeIfAbsent(
                WebSocketHandshakeLoggingInterceptor.RAW_INBOUND_MESSAGE_COUNT_ATTRIBUTE,
                ignored -> new AtomicInteger());
        int messageIndex = messageCount.incrementAndGet();
        if (messageIndex > RAW_MESSAGE_LOG_LIMIT_BEFORE_CONNECT) {
            if (messageIndex == RAW_MESSAGE_LOG_LIMIT_BEFORE_CONNECT + 1) {
                log.warn(
                        "[WS 디버그] raw 수신 메타데이터 로깅 제한 도달 sessionId={} traceId={} limit={}",
                        session.getId(),
                        value(attributes, WebSocketHandshakeLoggingInterceptor.TRACE_ID_ATTRIBUTE),
                        RAW_MESSAGE_LOG_LIMIT_BEFORE_CONNECT);
            }
            return;
        }

        InboundMessageMetadata metadata = metadata(message);
        log.info(
                "[WS 디버그] raw 수신 sessionId={} traceId={} messageIndex={} messageType={} payloadLength={} lastFragment={} startsWithConnect={} endsWithNull={} heartbeat={} finalByteHex={}",
                session.getId(),
                value(attributes, WebSocketHandshakeLoggingInterceptor.TRACE_ID_ATTRIBUTE),
                messageIndex,
                metadata.messageType(),
                message.getPayloadLength(),
                message.isLast(),
                metadata.startsWithConnect(),
                metadata.endsWithNull(),
                metadata.heartbeat(),
                metadata.finalByteHex());
    }

    private static InboundMessageMetadata metadata(WebSocketMessage<?> message) {
        if (message instanceof TextMessage textMessage) {
            String payload = textMessage.getPayload();
            return new InboundMessageMetadata(
                    TextMessage.class.getSimpleName(),
                    startsWithCommand(payload),
                    !payload.isEmpty() && payload.charAt(payload.length() - 1) == '\0',
                    "\n".equals(payload) || "\r\n".equals(payload),
                    payload.isEmpty() ? "-" : String.format("%04X", (int) payload.charAt(payload.length() - 1)));
        }

        if (message instanceof BinaryMessage binaryMessage) {
            ByteBuffer payload = binaryMessage.getPayload().asReadOnlyBuffer();
            int lastByte = payload.hasRemaining() ? Byte.toUnsignedInt(payload.get(payload.limit() - 1)) : -1;
            return new InboundMessageMetadata(
                    BinaryMessage.class.getSimpleName(),
                    startsWithCommand(payload),
                    lastByte == 0,
                    isHeartbeat(payload),
                    lastByte < 0 ? "-" : String.format("%02X", lastByte));
        }

        return new InboundMessageMetadata(message.getClass().getSimpleName(), false, false, false, "-");
    }

    private static boolean startsWithCommand(String payload) {
        return payload.startsWith("CONNECT") || payload.startsWith("STOMP");
    }

    private static boolean startsWithCommand(ByteBuffer payload) {
        return startsWith(payload, CONNECT_COMMAND) || startsWith(payload, STOMP_COMMAND);
    }

    private static boolean startsWith(ByteBuffer payload, byte[] expected) {
        if (payload.remaining() < expected.length) {
            return false;
        }
        int position = payload.position();
        for (int i = 0; i < expected.length; i++) {
            if (payload.get(position + i) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHeartbeat(ByteBuffer payload) {
        int remaining = payload.remaining();
        int position = payload.position();
        return (remaining == 1 && payload.get(position) == '\n')
                || (remaining == 2 && payload.get(position) == '\r' && payload.get(position + 1) == '\n');
    }

    private static long durationMillis(Map<String, Object> attributes) {
        Object startedAt = attributes.get(WebSocketHandshakeLoggingInterceptor.HANDSHAKE_STARTED_AT_ATTRIBUTE);
        return startedAt instanceof Long startedAtNanos ? (System.nanoTime() - startedAtNanos) / 1_000_000 : -1;
    }

    private static String value(Map<String, Object> attributes, String key) {
        return String.valueOf(attributes.getOrDefault(key, "-"));
    }

    private record InboundMessageMetadata(
            String messageType,
            boolean startsWithConnect,
            boolean endsWithNull,
            boolean heartbeat,
            String finalByteHex) {}
}
