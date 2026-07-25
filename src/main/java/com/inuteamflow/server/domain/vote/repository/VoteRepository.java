package com.inuteamflow.server.domain.vote.repository;

import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.vote.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    List<Vote> findByTeam(Team team);

    boolean existsByCreatedByAndIsOpenedTrue(Long createdBy);

    @Modifying
    @Query("""
    DELETE FROM Vote v
    WHERE v.createdBy = :createdBy
      AND v.isOpened = false
    """)
    void deleteClosedByCreatedBy(@Param("createdBy") Long createdBy);

    @Modifying
    @Query("""
    DELETE FROM Vote v
    WHERE v.voteId = :voteId
    """)
    void deleteByVoteId(@Param("voteId") Long voteId);
}
