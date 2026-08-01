package com.inuteamflow.server.domain.fcm.dto;

import com.inuteamflow.server.domain.notification.enums.NotificationType;
import java.util.List;

public record FcmMultiEvent(
        List<Long> receiverIds, String title, String body, String redirectUrl, NotificationType type) {}
