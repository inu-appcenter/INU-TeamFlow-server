package com.inuteamflow.server.domain.fcm.dto;

import com.inuteamflow.server.domain.notification.enums.NotificationType;

import java.util.List;

public record ChatFcmEvent(
        List<Long> receiverIds,
        String title,
        String body,
        NotificationType type,
        String redirectUrl,
        Long roomId,
        String collapseKey
) {}
