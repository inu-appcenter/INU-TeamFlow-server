package com.inuteamflow.server.domain.push.dto;

import com.inuteamflow.server.domain.notification.enums.NotificationType;
import java.util.List;

public record PushMultiEvent(
        List<Long> receiverIds, String title, String body, String redirectUrl, NotificationType type) {}
