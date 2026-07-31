package com.inuteamflow.server.domain.event.dto;

import com.inuteamflow.server.domain.event.enums.RecurrenceEditScope;
import java.time.LocalDateTime;

public interface EventUpdateCommand extends EventCreateCommand {

    Boolean getIsFinished();

    RecurrenceEditScope getRecurrenceEditScope();

    LocalDateTime getOccurrenceAt();
}
