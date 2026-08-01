package com.inuteamflow.server.domain.invitation.repository;

import com.inuteamflow.server.domain.invitation.entity.TeamInvitation;
import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {

    Page<TeamInvitation> findByReceiver(User user, Pageable pageable);

    Page<TeamInvitation> findByCreatedBy(Long userId, Pageable pageable);

    // 특정 팀+수신자 조합으로 초대 조회
    Optional<TeamInvitation> findByTeam_TeamIdAndReceiver(Long teamTeamId, User receiver);

    List<TeamInvitation> findByTeamAndReceiverIn(Team team, List<User> receivers);

    void deleteAllByTeam(Team team);

    @Modifying
    @Query("DELETE FROM TeamInvitation ti WHERE ti.receiver = :receiver")
    void deleteByReceiver(@Param("receiver") User receiver);

    @Modifying
    @Query("DELETE FROM TeamInvitation ti WHERE ti.createdBy = :createdBy")
    void deleteByCreatedBy(@Param("createdBy") Long createdBy);
}
