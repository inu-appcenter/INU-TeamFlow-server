package com.inuteamflow.server.domain.vote.repository;

import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.vote.entity.VoteAvailability;
import com.inuteamflow.server.domain.vote.entity.VoteParticipant;
import com.inuteamflow.server.domain.vote.entity.VoteTimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VoteAvailabilityRepository extends JpaRepository<VoteAvailability, Long> {

    @Query("""
            SELECT va FROM VoteAvailability va
            JOIN FETCH va.voteParticipant vp JOIN FETCH vp.teamMember
            WHERE va.voteTimeSlot IN :voteTimeSlots
            """)
    List<VoteAvailability> findByVoteTimeSlotIn(@Param("voteTimeSlots") List<VoteTimeSlot> voteTimeSlots);

    @Query("""
            SELECT va.voteTimeSlot.voteTimeSlotId, COUNT(va)
            FROM VoteAvailability va
            WHERE va.voteTimeSlot.voteDate.vote.voteId = :voteId
            GROUP BY va.voteTimeSlot.voteTimeSlotId
            """)
    List<Object[]> countGroupByTimeSlotId(@Param("voteId") Long voteId);

    void deleteByVoteParticipant(VoteParticipant voteParticipant);

    void deleteByVoteParticipant_TeamMember_User(User voteParticipantTeamMemberUser);
}
