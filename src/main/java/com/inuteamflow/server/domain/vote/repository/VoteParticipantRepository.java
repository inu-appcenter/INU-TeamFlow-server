package com.inuteamflow.server.domain.vote.repository;

import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.vote.entity.Vote;
import com.inuteamflow.server.domain.vote.entity.VoteParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VoteParticipantRepository extends JpaRepository<VoteParticipant, Long> {

    @Query("""
            SELECT vp FROM VoteParticipant vp
            JOIN FETCH vp.teamMember tm JOIN FETCH tm.user
            WHERE vp.vote = :vote
            """)
    List<VoteParticipant> findByVote(@Param("vote") Vote vote);

    Optional<VoteParticipant> findByVoteAndTeamMember(Vote vote, TeamMember teamMember);

    void deleteByTeamMember_User(User teamMemberUser);
}
