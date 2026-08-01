package com.inuteamflow.server.domain.event.entity;

import com.inuteamflow.server.domain.event.dto.EventCreateCommand;
import com.inuteamflow.server.domain.event.dto.EventUpdateCommand;
import com.inuteamflow.server.domain.event.dto.Recurrence;
import com.inuteamflow.server.domain.event.enums.EventColor;
import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.global.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "is_all_day")
    private Boolean isAllDay;

    @Column(name = "color")
    @Enumerated(EnumType.STRING)
    private EventColor color;

    @Column(name = "uid")
    private String uid;

    @Column(name = "sequence")
    private Integer sequence;

    @Column(name = "is_finished")
    private Boolean isFinished;

    @Column(name = "is_single")
    private Boolean isSingle;

    @Builder
    private Event(
            Team team,
            String title,
            String description,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Boolean isAllDay,
            EventColor color,
            String uid,
            Integer sequence,
            Boolean isFinished,
            Boolean isSingle) {
        this.team = team;
        this.title = title;
        this.description = description;
        this.startAt = startAt;
        this.endAt = endAt;
        this.isAllDay = isAllDay;
        this.color = color;
        this.uid = uid;
        this.sequence = sequence;
        this.isFinished = isFinished;
        this.isSingle = isSingle;
    }

    public static Event create(EventCreateCommand command) {
        return Event.builder()
                .title(command.getTitle())
                .description(command.getDescription())
                .startAt(command.getStartAt())
                .endAt(command.getEndAt())
                .isAllDay(command.getIsAllDay())
                .color(command.getColor())
                .uid(UUID.randomUUID().toString())
                .sequence(0)
                .isFinished(false)
                .isSingle(resolveIsSingle(command.getRecurrence()))
                .build();
    }

    public static Event create(Team team, EventCreateCommand command) {
        return Event.builder()
                .team(team)
                .title(command.getTitle())
                .description(command.getDescription())
                .startAt(command.getStartAt())
                .endAt(command.getEndAt())
                .isAllDay(command.getIsAllDay())
                .color(command.getColor())
                .uid(UUID.randomUUID().toString())
                .sequence(0)
                .isFinished(false)
                .isSingle(resolveIsSingle(command.getRecurrence()))
                .build();
    }

    public static Event createRecurring(EventUpdateCommand command) {
        return Event.builder()
                .title(command.getTitle())
                .description(command.getDescription())
                .startAt(command.getStartAt())
                .endAt(command.getEndAt())
                .isAllDay(command.getIsAllDay())
                .color(command.getColor())
                .uid(UUID.randomUUID().toString())
                .sequence(0)
                .isFinished(command.getIsFinished() != null ? command.getIsFinished() : false)
                .isSingle(false)
                .build();
    }

    public static Event createRecurring(Team team, EventUpdateCommand command) {
        return Event.builder()
                .team(team)
                .title(command.getTitle())
                .description(command.getDescription())
                .startAt(command.getStartAt())
                .endAt(command.getEndAt())
                .isAllDay(command.getIsAllDay())
                .color(command.getColor())
                .uid(UUID.randomUUID().toString())
                .sequence(0)
                .isFinished(command.getIsFinished() != null ? command.getIsFinished() : false)
                .isSingle(false)
                .build();
    }

    public void update(EventUpdateCommand command) {
        this.title = command.getTitle();
        this.description = command.getDescription();
        this.startAt = command.getStartAt();
        this.endAt = command.getEndAt();
        this.isAllDay = command.getIsAllDay();
        this.color = command.getColor();
        if (command.getIsFinished() != null) {
            this.isFinished = command.getIsFinished();
        }
    }

    public void changeToRecurring() {
        this.isSingle = false;
    }

    public void increaseSequence() {
        this.sequence++;
    }

    public Long getTeamId() {
        return team == null ? null : team.getTeamId();
    }

    private static Boolean resolveIsSingle(Recurrence recurrence) {
        return recurrence == null;
    }
}
