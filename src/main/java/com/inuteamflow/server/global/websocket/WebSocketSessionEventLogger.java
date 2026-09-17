package com.inuteamflow.server.global.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
public class WebSocketSessionEventLogger {

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        log.info(
                "[WS 디버그] 세션 CONNECTED sessionId={} user={}",
                event.getMessage().getHeaders().get("simpSessionId"),
                event.getUser()); // 임시 디버그용, 확인 후 제거
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        log.info(
                "[WS 디버그] 세션 종료 sessionId={} closeStatus={} user={}",
                event.getSessionId(),
                event.getCloseStatus(),
                event.getUser()); // 임시 디버그용, 확인 후 제거
    }
}
