package com.inuteamflow.server.domain.vote.repository;

import com.inuteamflow.server.domain.vote.entity.Vote;
import com.inuteamflow.server.domain.vote.entity.VoteResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoteResultRepository extends JpaRepository<VoteResult, Long> {

    boolean existsByVote(Vote vote);

    @Modifying
    @Query("""
    DELETE FROM VoteResult vr
    WHERE vr.vote.createdBy = :createdBy
      AND vr.vote.isOpened = false
    """)
    void deleteByClosedVoteCreatedBy(@Param("createdBy") Long createdBy);

    @Modifying
    @Query("""
    DELETE FROM VoteResult vr
    WHERE vr.vote.voteId = :voteId
    """)
    void deleteByVoteId(@Param("voteId") Long voteId);
}
