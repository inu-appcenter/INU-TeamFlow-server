package com.inuteamflow.server.domain.event.entity;

import com.inuteamflow.server.global.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "event_reminder_log", uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "occurrence_at"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventReminderLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_reminder_log_id")
    private Long eventReminderLogId;

    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "occurrence_at")
    private LocalDateTime occurrenceAt;

    @Builder
    private EventReminderLog(Long eventId, LocalDateTime occurrenceAt) {
        this.eventId = eventId;
        this.occurrenceAt = occurrenceAt;
    }

    public static EventReminderLog create(Long eventId, LocalDateTime occurrenceAt) {
        return EventReminderLog.builder()
                .eventId(eventId)
                .occurrenceAt(occurrenceAt)
                .build();
    }
}
