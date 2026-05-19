package com.inuteamflow.server.domain.invitation.entity;

import com.inuteamflow.server.domain.invitation.enums.InvitationStatus;
import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "team_invitation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamInvitation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_invitation_id")
    private Long teamInvitationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(name = "invite_status", nullable = false)
    private InvitationStatus invitationStatus;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Builder
    private TeamInvitation(Team team, User receiver, InvitationStatus invitationStatus) {
        this.team = team;
        this.receiver = receiver;
        this.invitationStatus = invitationStatus;
    }

    public static TeamInvitation create(Team team, User receiver) {
        return TeamInvitation.builder()
                .team(team)
                .receiver(receiver)
                .invitationStatus(InvitationStatus.WAITING)
                .build();
    }

    // DECLINED 상태에서 재초대 시, 기존 레코드를 WAITING으로 초기화
    public void reinvite() {
        this.invitationStatus = InvitationStatus.WAITING;
        this.respondedAt = null;
    }

    public void accept() {
        this.invitationStatus = InvitationStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void decline() {
        this.invitationStatus = InvitationStatus.DECLINED;
        this.respondedAt = LocalDateTime.now();
    }

    public Long getTeamId() {
        return team.getTeamId();
    }

    public Long getReceiverId() {
        return receiver.getUserId();
    }

}
