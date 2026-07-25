package com.inuteamflow.server.domain.vote.repository;

import com.inuteamflow.server.domain.vote.entity.Vote;
import com.inuteamflow.server.domain.vote.entity.VoteDate;
import com.inuteamflow.server.domain.vote.entity.VoteTimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface VoteTimeSlotRepository extends JpaRepository<VoteTimeSlot, Long> {

    @Query("""
            select vts
            from VoteTimeSlot vts
            where vts.voteDate in :voteDates
            order by vts.voteDate.date asc, vts.slotStartAt asc
            """)
    List<VoteTimeSlot> findByVoteDatesOrderByDateAndStartAt(@Param("voteDates") List<VoteDate> voteDates);

    @Query("""
            select vts
            from VoteTimeSlot vts
            where vts.voteDate.vote = :vote
              and vts.voteDate.date = :date
              and vts.slotStartAt >= :startTime
              and vts.slotEndAt <= :endTime
            order by vts.slotStartAt asc
            """)
    List<VoteTimeSlot> findByVoteAndDateAndTimeRange(
            @Param("vote") Vote vote,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    @Query("""
            select vts from VoteTimeSlot vts
            where vts.voteTimeSlotId in :ids
            and vts.voteDate.vote = :vote
            """)
    List<VoteTimeSlot> findByIdsAndVote(@Param("ids") List<Long> ids, @Param("vote") Vote vote);

    @Modifying
    @Query("""
    DELETE FROM VoteTimeSlot vts
    WHERE vts.voteDate.vote.createdBy = :createdBy
      AND vts.voteDate.vote.isOpened = false
    """)
    void deleteByClosedVoteCreatedBy(@Param("createdBy") Long createdBy);

    @Modifying
    @Query("""
    DELETE FROM VoteTimeSlot vts
    WHERE vts.voteDate.vote.voteId = :voteId
    """)
    void deleteByVoteId(@Param("voteId") Long voteId);
}
