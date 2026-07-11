package com.inuteamflow.server.domain.event.entity;

import com.inuteamflow.server.domain.event.enums.EventRole;
import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.global.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "recurrence_exception_participant",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"recurrence_exception_id", "team_member_id"}
        ))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecurrenceExceptionParticipant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recurrence_exception_participant_id")
    private Long recurrenceExceptionParticipantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurrence_exception_id", nullable = false)
    private RecurrenceException recurrenceException;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_member_id", nullable = false)
    private TeamMember teamMember;

    @Column(name = "event_role")
    @Enumerated(EnumType.STRING)
    private EventRole eventRole;

    @Builder
    private RecurrenceExceptionParticipant(
            RecurrenceException recurrenceException,
            TeamMember teamMember,
            EventRole eventRole
    ) {
        this.recurrenceException = recurrenceException;
        this.teamMember = teamMember;
        this.eventRole = eventRole;
    }

    public static RecurrenceExceptionParticipant create(
            RecurrenceException recurrenceException,
            TeamMember teamMember,
            EventRole eventRole
    ) {
        return RecurrenceExceptionParticipant.builder()
                .recurrenceException(recurrenceException)
                .teamMember(teamMember)
                .eventRole(eventRole)
                .build();
    }

}
