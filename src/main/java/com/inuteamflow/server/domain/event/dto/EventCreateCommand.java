package com.inuteamflow.server.domain.event.dto;

import com.inuteamflow.server.domain.event.enums.EventColor;
import java.time.LocalDateTime;

public interface EventCreateCommand {

    String getTitle();

    String getDescription();

    LocalDateTime getStartAt();

    LocalDateTime getEndAt();

    Boolean getIsAllDay();

    EventColor getColor();

    Recurrence getRecurrence();
}
