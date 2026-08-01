package com.inuteamflow.server.domain.vote.entity;

import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.global.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "vote_participant", uniqueConstraints = @UniqueConstraint(columnNames = {"vote_id", "team_member_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoteParticipant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vote_participant_id")
    private Long voteParticipantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_id")
    private Vote vote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_member_id")
    private TeamMember teamMember;

    @Column(name = "has_completed")
    private Boolean hasCompleted;

    @Builder
    private VoteParticipant(Vote vote, TeamMember teamMember, Boolean hasCompleted) {
        this.vote = vote;
        this.teamMember = teamMember;
        this.hasCompleted = hasCompleted;
    }

    public static VoteParticipant create(Vote vote, TeamMember teamMember) {
        return VoteParticipant.builder()
                .vote(vote)
                .teamMember(teamMember)
                .hasCompleted(false)
                .build();
    }

    public void complete() {
        this.hasCompleted = true;
    }
}
