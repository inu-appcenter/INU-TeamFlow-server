package com.inuteamflow.server.domain.invitation.service;

import com.inuteamflow.server.domain.invitation.dto.request.TeamInvitationCreateRequest;
import com.inuteamflow.server.domain.invitation.dto.request.TeamInvitationStatusUpdateRequest;
import com.inuteamflow.server.domain.invitation.dto.response.TeamInvitationResponse;
import com.inuteamflow.server.domain.invitation.entity.TeamInvitation;
import com.inuteamflow.server.domain.invitation.enums.InvitationDirection;
import com.inuteamflow.server.domain.invitation.enums.InvitationStatus;
import com.inuteamflow.server.domain.invitation.repository.TeamInvitationRepository;
import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.domain.team.enums.TeamRole;
import com.inuteamflow.server.domain.team.repository.TeamMemberRepository;
import com.inuteamflow.server.domain.team.repository.TeamRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamInvitationService {

    private final TeamInvitationRepository teamInvitationRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    // 내가 받은/보낸 팀 초대 목록
    public Page<TeamInvitationResponse> getInvitations(User user, InvitationDirection direction, Pageable pageable) {
        if (direction == InvitationDirection.RECEIVED) {
            return teamInvitationRepository.findByReceiver(user, pageable)
                    .map(invitation -> {
                        String senderName = invitation.getCreatedBy() == null ? null :
                                userRepository.findById(invitation.getCreatedBy())
                                        .map(User::getName)
                                        .orElse(null);
                        return TeamInvitationResponse.from(invitation, senderName);
                    });
        } else {
            return teamInvitationRepository.findByCreatedBy(user.getUserId(), pageable)
                    .map(invitation -> TeamInvitationResponse.from(invitation, user.getName()));
        }
    }

    // 팀 초대하기
    @Transactional
    public TeamInvitationResponse invite(User sender, Long teamId, TeamInvitationCreateRequest request) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));

        TeamMember senderMember = teamMemberRepository.findByTeamAndUser(team, sender)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));

        if (senderMember.getTeamRole() != TeamRole.LEADER) {
            throw new RestApiException(CustomErrorCode.INVITATION_FORBIDDEN);
        }

        User receiver = userRepository.findByStudentNumber(request.getStudentNumber())
                .orElseThrow(() -> new RestApiException(CustomErrorCode.USER_NOT_FOUND));

        if (receiver.getUserId().equals(sender.getUserId())) {
            throw new RestApiException(CustomErrorCode.INVITATION_SELF_INVITE);
        }

        if (!Boolean.TRUE.equals(receiver.getIsSchoolVerified())) {
            throw new RestApiException(CustomErrorCode.INVITATION_RECEIVER_NOT_VERIFIED);
        }

        if (teamMemberRepository.findByTeamAndUser(team, receiver).isPresent()) {
            throw new RestApiException(CustomErrorCode.INVITATION_ALREADY_MEMBER);
        }

        TeamInvitation invitation = teamInvitationRepository.findByTeam_TeamIdAndReceiver(teamId, receiver)
                .map(existing -> {
                    if (existing.getInvitationStatus() == InvitationStatus.WAITING) {
                        throw new RestApiException(CustomErrorCode.INVITATION_ALREADY_SENT);
                    }
                    existing.reinvite();
                    return existing;
                })
                .orElseGet(() -> teamInvitationRepository.save(
                        TeamInvitation.create(team, receiver)
                ));

        return TeamInvitationResponse.from(invitation, sender.getName());

    }

    // 팀 초대 수락/거절
    @Transactional
    public TeamInvitationResponse updateStatus(User receiver, Long invitationId, TeamInvitationStatusUpdateRequest request) {
        TeamInvitation invitation = teamInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.INVITATION_NOT_FOUND));

        if (!invitation.getReceiverId().equals(receiver.getUserId())) {
            throw new RestApiException(CustomErrorCode.INVITATION_FORBIDDEN);
        }

        if (invitation.getInvitationStatus() != InvitationStatus.WAITING) {
            throw new RestApiException(CustomErrorCode.INVITATION_STATUS_INVALID);
        }

        InvitationStatus newStatus = request.getStatus();

        if (newStatus == InvitationStatus.ACCEPTED) {
            invitation.accept();
            teamMemberRepository.save(TeamMember.create(
                    invitation.getTeam(),
                    receiver,
                    TeamRole.MEMBER
            ));
        } else if (newStatus == InvitationStatus.DECLINED) {
            invitation.decline();
        } else {
            throw new RestApiException(CustomErrorCode.INVITATION_STATUS_INVALID);
        }

        String senderName = invitation.getCreatedBy() == null ? null :
                userRepository.findById(invitation.getCreatedBy())
                        .map(User::getName)
                        .orElse(null);
        return TeamInvitationResponse.from(invitation, senderName);

    }
}
