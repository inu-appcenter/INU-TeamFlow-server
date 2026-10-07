package com.inuteamflow.server.domain.push.dto;

import com.inuteamflow.server.domain.notification.enums.NotificationType;
import java.util.List;

public record ChatPushEvent(
        List<Long> receiverIds,
        String title,
        String body,
        NotificationType type,
        String redirectUrl,
        Long roomId,
        String collapseKey) {}
