package com.inuteamflow.server.domain.invitation.service;

import com.inuteamflow.server.domain.chat.service.ChatRoomService;
import com.inuteamflow.server.domain.invitation.dto.request.TeamInvitationCreateRequest;
import com.inuteamflow.server.domain.invitation.dto.request.TeamInvitationStatusUpdateRequest;
import com.inuteamflow.server.domain.invitation.dto.response.InvitationCandidateResponse;
import com.inuteamflow.server.domain.invitation.dto.response.TeamInvitationResponse;
import com.inuteamflow.server.domain.invitation.entity.TeamInvitation;
import com.inuteamflow.server.domain.invitation.enums.InvitationCandidateStatus;
import com.inuteamflow.server.domain.invitation.enums.InvitationDirection;
import com.inuteamflow.server.domain.invitation.repository.TeamInvitationRepository;
import com.inuteamflow.server.domain.notification.enums.NotificationType;
import com.inuteamflow.server.domain.notification.service.NotificationService;
import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.domain.team.enums.TeamRole;
import com.inuteamflow.server.domain.team.repository.TeamMemberRepository;
import com.inuteamflow.server.domain.team.repository.TeamRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.global.enums.Status;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamInvitationService {

    private final TeamInvitationRepository teamInvitationRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final ChatRoomService chatRoomService;
    private final NotificationService notificationService;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 사용자가 받은 또는 보낸 팀 초대 목록을 조회한다.
     *
     * <p>{@link InvitationDirection#RECEIVED}이면 사용자가 수신자인 초대를, 그 외에는 사용자가 발신자인
     * 초대를 조회한다.</p>
     *
     * @param user 초대 목록을 조회할 사용자
     * @param direction 조회할 초대 방향
     * @param pageable 페이지 정보
     * @return 발신자 이름을 포함한 팀 초대 목록
     */
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

    /**
     * 학번으로 사용자를 찾아 팀에 초대한다.
     *
     * <p>팀 리더만 초대할 수 있으며, 본인을 초대하거나 학교 인증되지 않은 사용자를 초대하거나 이미 팀원인
     * 사용자를 초대할 수 없다. 대기 중인 초대가 없는 상태로 이전에 초대했던 기록이 있으면 그 초대를 재사용하고,
     * 대기 중인 초대가 이미 있으면 중복 초대로 거부한다. 초대 생성 후 수신자에게 알림을 보낸다.</p>
     *
     * @param sender 초대를 보내는 사용자
     * @param teamId 초대할 팀 ID
     * @param request 초대 대상의 학번을 담은 요청
     * @return 생성되었거나 재사용된 팀 초대 정보
     * @throws RestApiException 팀을 찾을 수 없거나, 발신자가 팀 멤버가 아니거나, 발신자가 리더가 아니거나,
     *                       대상 사용자를 찾을 수 없거나, 본인을 초대하려 하거나, 대상이 학교 인증되지 않았거나,
     *                       대상이 이미 팀원이거나, 이미 대기 중인 초대가 있는 경우
     */
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
                    if (existing.getInvitationStatus() == Status.WAITING) {
                        throw new RestApiException(CustomErrorCode.INVITATION_ALREADY_SENT);
                    }
                    existing.reinvite();
                    return existing;
                })
                .orElseGet(() -> teamInvitationRepository.save(
                        TeamInvitation.create(team, receiver)
                ));

        notificationService.createNotification(
                receiver,
                "[" + team.getName() + "] 팀에서 초대장이 도착했어요",
                sender.getName() + "님이 팀에 초대했어요",
                NotificationType.INVITE,
                "/mypage/invitations"
        );

        return TeamInvitationResponse.from(invitation, sender.getName());

    }

    /**
     * 이름으로 팀 초대 후보를 검색하고 현재 초대 상태를 조회한다.
     *
     * <p>현재 팀원은 {@link InvitationCandidateStatus#MEMBER}, 대기 중인 초대가 존재하면
     * {@link InvitationCandidateStatus#PENDING}, 그 외에는 {@link InvitationCandidateStatus#NONE}으로 판정한다.</p>
     *
     * @param user 초대 후보 검색을 요청한 사용자
     * @param teamId 초대 후보를 검색할 팀 ID
     * @param name 검색할 사용자 이름
     * @return 사용자별 현재 초대 상태를 포함한 초대 후보 목록
     * @throws RestApiException 팀을 찾을 수 없거나 사용자가 해당 팀의 멤버가 아닌 경우
     */
    public List<InvitationCandidateResponse> getCandidates(
            User user,
            Long teamId,
            String name
    ) {
        // 팀 내부 기능이므로 팀 존재 여부와 요청자의 팀 소속 여부를 먼저 검증한다.
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));

        teamMemberRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));

        // 학교 인증 사용자 중 요청자 본인을 제외하고 이름이 일치하는 후보를 검색한다.
        List<User> candidates = userRepository.searchByName(name, user.getUserId());
        if (candidates.isEmpty()) {
            return List.of();
        }

        // 후보별 반복 조회를 피하기 위해 현재 팀원과 기존 초대 기록을 각각 일괄 조회한다.
        List<TeamMember> members = teamMemberRepository.findByTeamAndUserIn(team, candidates);
        List<TeamInvitation> invitations = teamInvitationRepository.findByTeamAndReceiverIn(team, candidates);

        // 상태 판정에 필요한 사용자 ID만 집합으로 변환한다.
        Set<Long> memberIds = members.stream()
                .map(TeamMember::getUser)
                .map(User::getUserId)
                .collect(Collectors.toSet());

        Set<Long> pendingIds = invitations.stream()
                .filter(invitation -> invitation.getInvitationStatus() == Status.WAITING)
                .map(TeamInvitation::getReceiverId)
                .collect(Collectors.toSet());

        // 현재 팀원 여부를 우선하고, 대기 중인 초대 여부를 기준으로 응답 상태를 결정한다.
        return candidates.stream()
                .map(candidate -> InvitationCandidateResponse.from(
                        candidate,
                        resolveCandidateStatus(candidate.getUserId(), memberIds, pendingIds)
                ))
                .toList();
    }

    /**
     * 받은 팀 초대를 수락하거나 거절한다.
     *
     * <p>초대 수신자 본인만 처리할 수 있고, 대기 중인 초대만 처리할 수 있다. 수락 시 팀 멤버로 등록하고
     * 팀 채팅방에 추가하며, 수락/거절 결과를 초대 발신자에게 알림으로 보낸다.</p>
     *
     * @param receiver 초대를 처리하는 사용자
     * @param invitationId 처리할 초대 ID
     * @param request 적용할 초대 상태({@link Status#ACCEPTED} 또는 {@link Status#DECLINED})
     * @return 처리된 팀 초대 정보
     * @throws RestApiException 초대를 찾을 수 없거나, 발신자를 찾을 수 없거나, 처리 요청자가 초대 수신자가
     *                       아니거나, 초대가 대기 중 상태가 아니거나, 요청 상태가 수락/거절이 아닌 경우
     */
    @Transactional
    public TeamInvitationResponse updateStatus(User receiver, Long invitationId, TeamInvitationStatusUpdateRequest request) {
        TeamInvitation invitation = teamInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.INVITATION_NOT_FOUND));

        User sender = userRepository.findById(invitation.getCreatedBy())
                .orElseThrow(() -> new RestApiException(CustomErrorCode.USER_NOT_FOUND));

        if (!invitation.getReceiverId().equals(receiver.getUserId())) {
            throw new RestApiException(CustomErrorCode.INVITATION_FORBIDDEN);
        }

        if (invitation.getInvitationStatus() != Status.WAITING) {
            throw new RestApiException(CustomErrorCode.INVITATION_STATUS_INVALID);
        }

        Status newStatus = request.getStatus();

        if (newStatus == Status.ACCEPTED) {
            invitation.accept();
            teamMemberRepository.save(TeamMember.create(
                    invitation.getTeam(),
                    receiver,
                    TeamRole.MEMBER
            ));
            chatRoomService.addTeamChatRoomMember(invitation.getTeam(), receiver);
            notificationService.createNotification(
                    sender,
                    "[" + invitation.getTeam().getName()+ "] 팀에 새 팀원이 합류했어요",
                    receiver.getName() + "님이 초대를 수락했어요",
                    NotificationType.INVITE,
                    "/team/" + invitation.getTeam().getTeamId()
            );
        } else if (newStatus == Status.DECLINED) {
            invitation.decline();
            notificationService.createNotification(
                    sender,
                    "[" + invitation.getTeam().getName() + "] 초대가 거절됐어요",
                    receiver.getName() + "님이 초대를 거절했어요",
                    NotificationType.INVITE,
                    "/mypage/invitations/"
            );
        } else {
            throw new RestApiException(CustomErrorCode.INVITATION_STATUS_INVALID);
        }

        return TeamInvitationResponse.from(invitation, sender.getName());

    }


    /**
     * 보낸 팀 초대를 취소한다.
     *
     * @param sender 초대를 취소하는 사용자
     * @param teamId 초대가 속한 팀 ID
     * @param invitationId 취소할 초대 ID
     * @return 취소된 팀 초대 정보
     * @throws RestApiException 팀을 찾을 수 없거나, 사용자가 팀 멤버가 아니거나, 사용자가 리더가 아니거나,
     *                       초대를 찾을 수 없는 경우
     */
    @Transactional
    public TeamInvitationResponse cancelInvitation(User sender, Long teamId, Long invitationId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));

        TeamMember senderMember = teamMemberRepository.findByTeamAndUser(team, sender)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));

        if (senderMember.getTeamRole() != TeamRole.LEADER) {
            throw new RestApiException(CustomErrorCode.INVITATION_FORBIDDEN);
        }

        TeamInvitation invitation = teamInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.INVITATION_NOT_FOUND));


        invitation.cancel();

        return TeamInvitationResponse.from(invitation, sender.getName());
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 후보 사용자의 현재 팀 소속 여부와 초대 대기 여부로 초대 상태를 결정한다.
     *
     * <p>후보 사용자가 현재 팀원이면서 대기 중인 초대도 존재하는 경우
     * {@link InvitationCandidateStatus#MEMBER}를 우선한다.</p>
     *
     * @param candidateId 초대 상태를 결정할 후보 사용자 ID
     * @param memberIds 현재 팀에 소속된 후보 사용자 ID 집합
     * @param pendingInvitationReceiverIds 대기 중인 초대를 받은 후보 사용자 ID 집합
     * @return 후보 사용자의 현재 초대 상태
     */
    private InvitationCandidateStatus resolveCandidateStatus(
            Long candidateId,
            Set<Long> memberIds,
            Set<Long> pendingInvitationReceiverIds
    ) {
        if (memberIds.contains(candidateId)) {
            return InvitationCandidateStatus.MEMBER;
        }
        if (pendingInvitationReceiverIds.contains(candidateId)) {
            return InvitationCandidateStatus.PENDING;
        }
        return InvitationCandidateStatus.NONE;
    }
}
