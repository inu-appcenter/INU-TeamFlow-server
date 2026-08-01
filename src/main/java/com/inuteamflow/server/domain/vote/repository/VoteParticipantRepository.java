package com.inuteamflow.server.domain.vote.repository;

import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.vote.entity.Vote;
import com.inuteamflow.server.domain.vote.entity.VoteParticipant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoteParticipantRepository extends JpaRepository<VoteParticipant, Long> {

    @Query("""
            SELECT vp FROM VoteParticipant vp
            JOIN FETCH vp.teamMember tm JOIN FETCH tm.user
            WHERE vp.vote = :vote
            """)
    List<VoteParticipant> findByVote(@Param("vote") Vote vote);

    Optional<VoteParticipant> findByVoteAndTeamMember(Vote vote, TeamMember teamMember);

    @Modifying
    @Query("""
    DELETE FROM VoteParticipant vp
    WHERE vp.teamMember.user = :user
    """)
    void deleteByTeamMemberUser(@Param("user") User user);

    @Modifying
    @Query("""
    DELETE FROM VoteParticipant vp
    WHERE vp.vote.createdBy = :createdBy
      AND vp.vote.isOpened = false
    """)
    void deleteByClosedVoteCreatedBy(@Param("createdBy") Long createdBy);

    void deleteByTeamMember(TeamMember teamMember);

    @Modifying
    @Query("""
    DELETE FROM VoteParticipant vp
    WHERE vp.vote.voteId = :voteId
    """)
    void deleteByVoteId(@Param("voteId") Long voteId);

    @Query("""
    SELECT CASE WHEN COUNT(vp) > 0 THEN true ELSE false END
    FROM VoteParticipant vp
    WHERE vp.vote.voteId = :voteId
      AND vp.teamMember.user.userId = :userId
    """)
    boolean existsByVoteIdAndUserId(@Param("voteId") Long voteId, @Param("userId") Long userId);

    @Query("""
    SELECT vp.vote
    FROM VoteParticipant vp
    WHERE vp.teamMember.user = :user
    """)
    List<Vote> findVotesByUser(@Param("user") User user);
}
