package com.inuteamflow.server.domain.invitation.repository;

import com.inuteamflow.server.domain.invitation.entity.TeamInvitation;
import com.inuteamflow.server.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {

    Page<TeamInvitation> findByReceiver(User user, Pageable pageable);

    Page<TeamInvitation> findByCreatedBy(Long userId, Pageable pageable);

    // 특정 팀+수신자 조합으로 초대 조회
    Optional<TeamInvitation> findByTeam_TeamIdAndReceiver(Long teamTeamId, User receiver);
}
