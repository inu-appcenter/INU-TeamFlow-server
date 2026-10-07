package com.inuteamflow.server.domain.push.service;

import com.inuteamflow.server.domain.push.dto.ChatPushEvent;
import com.inuteamflow.server.domain.push.dto.PushMultiEvent;
import com.inuteamflow.server.domain.push.dto.PushSingleEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PushEventListener {

    private final PushService pushService;

    /**
     * 단일 수신자 푸시 이벤트를 처리한다.
     *
     * <p>알림 저장 트랜잭션이 커밋된 후 푸시 전용 스레드에서 단일 사용자에게 푸시 알림을 발송한다.</p>
     *
     * @param event 단일 수신자 푸시 이벤트
     */
    @Async("pushTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSingle(PushSingleEvent event) {
        pushService.sendToUser(
                event.receiverId(),
                event.title(),
                event.body(),
                event.redirectUrl(),
                event.type(),
                event.notificationId());
    }

    /**
     * 다중 수신자 푸시 이벤트를 처리한다.
     *
     * <p>알림 저장 트랜잭션이 커밋된 후 푸시 전용 스레드에서 여러 사용자에게 푸시 알림을 발송한다.</p>
     *
     * @param event 다중 수신자 푸시 이벤트
     */
    @Async("pushTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMulti(PushMultiEvent event) {
        pushService.sendToUsers(event.receiverIds(), event.title(), event.body(), event.redirectUrl(), event.type());
    }

    /**
     * 채팅 푸시 이벤트를 처리한다.
     *
     * <p>호출 트랜잭션이 커밋된 후 푸시 전용 스레드에서 채팅방 축약 키가 포함된 푸시 알림을 발송한다.</p>
     *
     * @param event 채팅 푸시 이벤트
     */
    @Async("pushTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChat(ChatPushEvent event) {
        pushService.sendChatNotification(
                event.receiverIds(),
                event.title(),
                event.body(),
                event.type(),
                event.redirectUrl(),
                event.roomId(),
                event.collapseKey());
    }
}
