package com.inuteamflow.server.domain.fcm.dto;

import com.inuteamflow.server.domain.notification.enums.NotificationType;

public record FcmSingleEvent(
        Long receiverId,
        String title,
        String body,
        String redirectUrl,
        NotificationType type,
        Long notificationId
) {}
