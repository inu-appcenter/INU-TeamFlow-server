package com.inuteamflow.server.domain.teamNotice.repository;

import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.teamNotice.entity.TeamNotice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamNoticeRepository extends JpaRepository<TeamNotice, Long> {

    List<TeamNotice> findAllByTeam(Team team);

    Optional<TeamNotice> findByTeamNoticeIdAndTeam(Long teamNoticeId, Team team);

    @Query(
            value =
                    "SELECT n FROM TeamNotice n JOIN FETCH n.team WHERE n.team = :team ORDER BY n.isPinned DESC, n.createdAt DESC",
            countQuery = "SELECT COUNT(n) FROM TeamNotice n WHERE n.team = :team")
    Page<TeamNotice> findByTeam(@Param("team") Team team, Pageable pageable);

    @Query(
            value = "SELECT n FROM TeamNotice n JOIN FETCH n.team WHERE n.team IN :teams ORDER BY n.createdAt DESC",
            countQuery = "SELECT COUNT(n) FROM TeamNotice n WHERE n.team IN :teams")
    Page<TeamNotice> findByTeamIn(@Param("teams") List<Team> teams, Pageable pageable);

    @Query(
            value =
                    "SELECT n FROM TeamNotice n JOIN FETCH n.team WHERE n.createdBy = :createdBy ORDER BY n.createdAt DESC",
            countQuery = "SELECT COUNT(n) FROM TeamNotice n WHERE n.createdBy = :createdBy")
    Page<TeamNotice> findByCreatedBy(@Param("createdBy") Long createdBy, Pageable pageable);

    @Modifying
    @Query("DELETE FROM TeamNotice tn WHERE tn.createdBy = :createdBy")
    void deleteByCreatedBy(@Param("createdBy") Long createdBy);
}
