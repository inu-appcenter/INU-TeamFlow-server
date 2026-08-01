package com.inuteamflow.server.domain.team.entity;

import com.inuteamflow.server.domain.team.enums.TeamRole;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.global.BaseTimeEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

@Entity
@Getter
@Table(name = "team_member", uniqueConstraints = @UniqueConstraint(columnNames = {"team_id", "user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_member_id")
    private Long teamMemberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "team_role", nullable = false)
    private TeamRole teamRole;

    @Column(name = "joined_at", nullable = false)
    @CreatedDate
    private LocalDateTime joinedAt;

    @Builder
    private TeamMember(Team team, User user, TeamRole teamRole) {
        this.team = team;
        this.user = user;
        this.teamRole = teamRole;
    }

    public static TeamMember create(Team team, User user, TeamRole teamRole) {
        return TeamMember.builder().team(team).user(user).teamRole(teamRole).build();
    }

    public void updateRole(TeamRole role) {
        this.teamRole = role;
    }
}
