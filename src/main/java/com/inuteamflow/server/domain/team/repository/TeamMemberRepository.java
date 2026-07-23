package com.inuteamflow.server.domain.team.repository;

import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.domain.team.enums.TeamRole;
import com.inuteamflow.server.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    int countByTeam(Team team);
    List<TeamMember> findByUser(User user);
    List<TeamMember> findByTeam(Team team);
    Optional<TeamMember> findByTeamAndUser(Team team, User user);
    void deleteAllByTeam(Team team);

    @Query("""
    SELECT tm FROM TeamMember tm
    JOIN FETCH tm.team
    WHERE tm.user = :user
    """)
    List<TeamMember> findByUserWithTeam(@Param("user") User user);

    @Query("""
    SELECT tm.team.teamId, COUNT(tm)
    FROM TeamMember tm
    WHERE tm.team IN :teams
    GROUP BY tm.team.teamId
    """)
    List<Object[]> countByTeams(@Param("teams") List<Team> teams);

    @Query("""
    SELECT tm FROM TeamMember tm JOIN FETCH tm.user
    WHERE tm.team = :team AND tm.teamMemberId IN :ids
    """)
    List<TeamMember> findByTeamAndIds(@Param("team") Team team, @Param("ids") List<Long> ids);

    @Query("""
    SELECT tm FROM TeamMember tm JOIN FETCH tm.user
    WHERE tm.team = :team AND tm.user.userId = :userId
    """)
    Optional<TeamMember> findByTeamAndUserUserId(@Param("team") Team team, @Param("userId") Long userId);

    @Query("""
    SELECT tm FROM TeamMember tm JOIN FETCH tm.user
    WHERE tm.team = :team
    """)
    List<TeamMember> findByTeamWithUser(@Param("team") Team team);

    @Query("""
    SELECT tm FROM TeamMember tm
    JOIN FETCH tm.user JOIN FETCH tm.team
    WHERE tm.team IN :teams
    """)
    List<TeamMember> findByTeamInWithUser(@Param("teams") List<Team> teams);

    @Query("""
    SELECT tm.user FROM TeamMember tm
    WHERE tm.team = :team AND tm.user.userId != :excludeUserId
    """)
    List<User> findUsersByTeamExcluding(@Param("team") Team team, @Param("excludeUserId") Long excludeUserId);

    boolean existsByUserAndTeamRole(User user, TeamRole teamRole);

    @Modifying
    @Query("DELETE FROM TeamMember tm WHERE tm.user = :user")
    void deleteByUser(@Param("user") User user);
}
