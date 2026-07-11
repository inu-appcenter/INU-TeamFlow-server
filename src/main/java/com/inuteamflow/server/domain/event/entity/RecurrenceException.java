package com.inuteamflow.server.domain.event.entity;

import com.inuteamflow.server.domain.event.dto.EventUpdateCommand;
import com.inuteamflow.server.domain.event.enums.EventColor;
import com.inuteamflow.server.domain.event.enums.RecurrenceExceptionType;
import com.inuteamflow.server.global.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "recurrence_exception",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"event_id", "original_occurrence_at"}
        ))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecurrenceException extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recurrence_exception_id")
    private Long recurrenceExceptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "original_occurrence_at")
    private LocalDateTime originalOccurrenceAt;

    @Column(name = "exception_type")
    @Enumerated(EnumType.STRING)
    private RecurrenceExceptionType exceptionType;

    @Column(name = "modified_title")
    private String modifiedTitle;

    @Column(name = "modified_description")
    private String modifiedDescription;

    @Column(name = "modified_start_at")
    private LocalDateTime modifiedStartAt;

    @Column(name = "modified_end_at")
    private LocalDateTime modifiedEndAt;

    @Column(name = "modified_color")
    @Enumerated(EnumType.STRING)
    private EventColor modifiedColor;

    @Column(name = "modified_is_all_day")
    private Boolean modifiedIsAllDay;

    @Column(name = "modified_is_finished")
    private Boolean modifiedIsFinished;

    @Builder
    private RecurrenceException(
            Event event,
            LocalDateTime originalOccurrenceAt,
            RecurrenceExceptionType exceptionType,
            String modifiedTitle,
            String modifiedDescription,
            LocalDateTime modifiedStartAt,
            LocalDateTime modifiedEndAt,
            EventColor modifiedColor,
            Boolean modifiedAllDay,
            Boolean modifiedIsFinished
    ) {
        this.event = event;
        this.originalOccurrenceAt = originalOccurrenceAt;
        this.exceptionType = exceptionType;
        this.modifiedTitle = modifiedTitle;
        this.modifiedDescription = modifiedDescription;
        this.modifiedStartAt = modifiedStartAt;
        this.modifiedEndAt = modifiedEndAt;
        this.modifiedColor = modifiedColor;
        this.modifiedIsAllDay = modifiedAllDay;
        this.modifiedIsFinished = modifiedIsFinished;
    }

    public static RecurrenceException createModified(
            Event event,
            EventUpdateCommand command
    ) {
        return RecurrenceException.builder()
                .event(event)
                .originalOccurrenceAt(command.getOccurrenceAt())
                .exceptionType(RecurrenceExceptionType.MODIFIED)
                .modifiedTitle(command.getTitle())
                .modifiedDescription(command.getDescription())
                .modifiedStartAt(command.getStartAt())
                .modifiedEndAt(command.getEndAt())
                .modifiedColor(command.getColor())
                .modifiedAllDay(command.getIsAllDay())
                .modifiedIsFinished(command.getIsFinished())
                .build();
    }

    public static RecurrenceException createCancelled(
            Event event,
            LocalDateTime originalOccurrenceAt
    ) {
        return RecurrenceException.builder()
                .event(event)
                .originalOccurrenceAt(originalOccurrenceAt)
                .exceptionType(RecurrenceExceptionType.CANCELLED)
                .build();
    }

    public Long getEventId() {
        return event.getEventId();
    }

    public void update(
            EventUpdateCommand command
    ) {
        this.exceptionType = RecurrenceExceptionType.MODIFIED;
        this.modifiedTitle = command.getTitle();
        this.modifiedDescription = command.getDescription();
        this.modifiedStartAt = command.getStartAt();
        this.modifiedEndAt = command.getEndAt();
        this.modifiedColor = command.getColor();
        this.modifiedIsAllDay = command.getIsAllDay();
        this.modifiedIsFinished = command.getIsFinished();
    }

    public void cancel() {
        this.exceptionType = RecurrenceExceptionType.CANCELLED;
        this.modifiedTitle = null;
        this.modifiedDescription = null;
        this.modifiedStartAt = null;
        this.modifiedEndAt = null;
        this.modifiedColor = null;
        this.modifiedIsAllDay = false;
        this.modifiedIsFinished = false;
    }
}
