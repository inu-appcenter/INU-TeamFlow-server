package com.inuteamflow.server.domain.fcm.service;

import com.inuteamflow.server.domain.fcm.dto.ChatFcmEvent;
import com.inuteamflow.server.domain.fcm.dto.FcmMultiEvent;
import com.inuteamflow.server.domain.fcm.dto.FcmSingleEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class FcmEventListener {

    private final FcmService fcmService;

    /**
     * 단일 수신자 FCM 이벤트를 처리한다.
     *
     * <p>알림 저장 트랜잭션이 커밋된 후 단일 사용자에게 FCM 알림을 발송한다.</p>
     *
     * @param event 단일 수신자 FCM 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSingle(FcmSingleEvent event) {
        fcmService.sendToUser(
                event.receiverId(),
                event.title(),
                event.body(),
                event.redirectUrl(),
                event.type(),
                event.notificationId()
        );
    }

    /**
     * 다중 수신자 FCM 이벤트를 처리한다.
     *
     * <p>알림 저장 트랜잭션이 커밋된 후 여러 사용자에게 FCM 알림을 발송한다.</p>
     *
     * @param event 다중 수신자 FCM 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMulti(FcmMultiEvent event) {
        fcmService.sendToUsers(
                event.receiverIds(),
                event.title(),
                event.body(),
                event.redirectUrl(),
                event.type()
        );
    }

    /**
     * 채팅 FCM 이벤트를 처리한다.
     *
     * <p>호출 트랜잭션이 커밋된 후 채팅방 축약 키가 포함된 FCM 알림을 발송한다.</p>
     *
     * @param event 채팅 FCM 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChat(ChatFcmEvent event) {
        fcmService.sendChatNotification(
                event.receiverIds(),
                event.title(),
                event.body(),
                event.type(),
                event.redirectUrl(),
                event.roomId(),
                event.collapseKey()
        );
    }
}
