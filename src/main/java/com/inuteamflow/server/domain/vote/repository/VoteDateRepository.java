package com.inuteamflow.server.domain.vote.repository;

import com.inuteamflow.server.domain.vote.entity.Vote;
import com.inuteamflow.server.domain.vote.entity.VoteDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VoteDateRepository extends JpaRepository<VoteDate, Long> {

    List<VoteDate> findByVoteOrderByDateAsc(Vote vote);

    @Modifying
    @Query("""
    DELETE FROM VoteDate vd
    WHERE vd.vote.createdBy = :createdBy
      AND vd.vote.isOpened = false
    """)
    void deleteByClosedVoteCreatedBy(@Param("createdBy") Long createdBy);
}
