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
@Table(name = "event_participant", uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "team_member_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventParticipant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_participant_id")
    private Long eventParticipantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_member_id", nullable = false)
    private TeamMember teamMember;

    @Column(name = "event_role")
    @Enumerated(EnumType.STRING)
    private EventRole eventRole;

    @Builder
    private EventParticipant(Event event, TeamMember teamMember, EventRole eventRole) {
        this.event = event;
        this.teamMember = teamMember;
        this.eventRole = eventRole;
    }

    public static EventParticipant create(Event event, TeamMember teamMember, EventRole eventRole) {
        return EventParticipant.builder()
                .event(event)
                .teamMember(teamMember)
                .eventRole(eventRole)
                .build();
    }

    public Long getTeamMemberId() {
        return teamMember.getTeamMemberId();
    }
}
