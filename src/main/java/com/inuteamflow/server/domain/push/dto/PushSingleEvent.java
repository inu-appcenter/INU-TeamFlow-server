package com.inuteamflow.server.domain.push.dto;

import com.inuteamflow.server.domain.notification.enums.NotificationType;

public record PushSingleEvent(
        Long receiverId, String title, String body, String redirectUrl, NotificationType type, Long notificationId) {}
