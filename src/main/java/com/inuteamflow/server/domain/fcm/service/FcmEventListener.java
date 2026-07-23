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
