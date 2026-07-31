package com.inuteamflow.server.domain.team.service;

import com.inuteamflow.server.domain.chat.service.ChatRoomService;
import com.inuteamflow.server.domain.event.repository.EventParticipantRepository;
import com.inuteamflow.server.domain.event.repository.RecurrenceExceptionParticipantRepository;
import com.inuteamflow.server.domain.invitation.repository.TeamInvitationRepository;
import com.inuteamflow.server.domain.recruitment.entity.Recruitment;
import com.inuteamflow.server.domain.recruitment.repository.RecruitmentApplicationRepository;
import com.inuteamflow.server.domain.recruitment.repository.RecruitmentRepository;
import com.inuteamflow.server.domain.team.dto.request.TeamCreateRequest;
import com.inuteamflow.server.domain.team.dto.request.TeamUpdateRequest;
import com.inuteamflow.server.domain.team.dto.response.TeamDetailResponse;
import com.inuteamflow.server.domain.team.dto.response.TeamMemberResponse;
import com.inuteamflow.server.domain.team.dto.response.TeamSummaryResponse;
import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.domain.team.enums.TeamRole;
import com.inuteamflow.server.domain.team.repository.TeamMemberRepository;
import com.inuteamflow.server.domain.team.repository.TeamRepository;
import com.inuteamflow.server.domain.teamNotice.entity.TeamNotice;
import com.inuteamflow.server.domain.teamNotice.repository.TeamNoticeImageRepository;
import com.inuteamflow.server.domain.teamNotice.repository.TeamNoticeReadRepository;
import com.inuteamflow.server.domain.teamNotice.repository.TeamNoticeRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.domain.vote.repository.VoteAvailabilityRepository;
import com.inuteamflow.server.domain.vote.repository.VoteParticipantRepository;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import com.inuteamflow.server.global.s3.S3Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final S3Service s3Service;
    private final UserRepository userRepository;
    private final RecruitmentRepository recruitmentRepository;
    private final RecruitmentApplicationRepository recruitmentApplicationRepository;
    private final TeamInvitationRepository teamInvitationRepository;
    private final TeamNoticeRepository teamNoticeRepository;
    private final TeamNoticeReadRepository teamNoticeReadRepository;
    private final TeamNoticeImageRepository teamNoticeImageRepository;
    private final VoteAvailabilityRepository voteAvailabilityRepository;
    private final VoteParticipantRepository voteParticipantRepository;
    private final RecurrenceExceptionParticipantRepository recurrenceExceptionParticipantRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final ChatRoomService chatRoomService;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 사용자가 속한 팀 목록을 조회한다.
     *
     * @param user 팀 목록을 조회할 사용자
     * @return 팀별 이미지 URL과 멤버 수를 포함한 팀 요약 목록
     */
    public List<TeamSummaryResponse> getMyTeams(User user) {

        List<TeamMember> memberships = teamMemberRepository.findByUserWithTeam(user);

        List<Team> teams = memberships.stream().map(TeamMember::getTeam).toList();

        Map<Long, Long> countMap = teamMemberRepository.countByTeams(teams).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        return memberships.stream()
                .map(tm -> {
                    Team team = tm.getTeam();
                    int memberCount =
                            countMap.getOrDefault(team.getTeamId(), 0L).intValue();
                    String imageUrl = s3Service.getTeamImageUrl(team.getImageKey(), team.getCategory());
                    return TeamSummaryResponse.create(team, imageUrl, memberCount);
                })
                .toList();
    }

    /**
     * 팀 상세 정보를 조회한다.
     *
     * @param teamId 조회할 팀 ID
     * @param user 조회를 요청한 사용자
     * @return 요청자의 팀 내 역할, 이미지 URL, 멤버 수를 포함한 팀 상세 정보
     * @throws RestApiException 팀을 찾을 수 없거나 사용자가 팀 멤버가 아닌 경우
     */
    public TeamDetailResponse getTeamDetails(Long teamId, User user) {

        Team team =
                teamRepository.findById(teamId).orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));

        // 현재 로그인한 유저가 해당 팀의 멤버인지 확인
        TeamMember teamMember = teamMemberRepository
                .findByTeamAndUser(team, user)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));

        int memberCount = teamMemberRepository.countByTeam(team);
        String imageUrl = s3Service.getTeamImageUrl(team.getImageKey(), team.getCategory());

        return TeamDetailResponse.create(team, teamMember, imageUrl, memberCount);
    }

    /**
     * 팀 멤버 목록을 조회한다.
     *
     * <p>요청자가 해당 팀의 멤버인 경우에만 조회할 수 있다.</p>
     *
     * @param teamId 멤버 목록을 조회할 팀 ID
     * @param user 조회를 요청한 사용자
     * @return 팀 멤버 목록
     * @throws RestApiException 팀을 찾을 수 없거나 요청자가 팀 멤버가 아닌 경우
     */
    public List<TeamMemberResponse> getTeamMembers(Long teamId, User user) {

        Team team =
                teamRepository.findById(teamId).orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));

        teamMemberRepository
                .findByTeamAndUser(team, user)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));

        return teamMemberRepository.findByTeam(team).stream()
                .map(tm -> TeamMemberResponse.create(tm, tm.getUser()))
                .toList();
    }

    /**
     * 팀원의 역할을 변경한다.
     *
     * <p>팀 리더만 변경할 수 있다. {@code newRole}이 {@link TeamRole#LEADER}이면 리더 위임으로 처리되어
     * 기존 리더는 {@link TeamRole#MEMBER}로 내려가고 대상이 새 리더가 되며, 그 외에는 매니저 지정/해제로
     * 대상의 역할만 변경한다.</p>
     *
     * @param user 역할 변경을 요청한 사용자
     * @param teamId 대상 팀 ID
     * @param targetMemberId 역할을 변경할 팀 멤버 ID
     * @param newRole 적용할 새 역할
     * @throws RestApiException 팀을 찾을 수 없거나, 요청자가 팀 멤버가 아니거나, 요청자가 리더가 아니거나,
     *                       대상 멤버를 찾을 수 없거나, 대상 멤버가 해당 팀 소속이 아니거나, 대상이 이미 같은
     *                       역할인 경우
     */
    @Transactional
    public void updateMemberRole(User user, Long teamId, Long targetMemberId, TeamRole newRole) {

        Team team =
                teamRepository.findById(teamId).orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));

        TeamMember requester = teamMemberRepository
                .findByTeamAndUser(team, user)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));

        if (requester.getTeamRole() != TeamRole.LEADER) {
            throw new RestApiException(CustomErrorCode.TEAM_FORBIDDEN);
        }

        TeamMember targetMember = teamMemberRepository
                .findById(targetMemberId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));

        if (!targetMember.getTeam().getTeamId().equals(teamId)) {
            throw new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND);
        }

        // 이미 같은 역할
        if (targetMember.getTeamRole() == newRole) {
            throw new RestApiException(CustomErrorCode.TEAM_MEMBER_ALREADY_ROLE);
        }

        // 리더 위임: 기존 리더(requester) 는 MEMBER 로 내려가고, target이 새 리더가 됨
        // (target이 MANAGER였든 MEMBER였든 상관없이 그대로 LEADER로 승격)
        if (newRole == TeamRole.LEADER) {
            requester.updateRole(TeamRole.MEMBER);
            targetMember.updateRole(TeamRole.LEADER);
            return;
        }

        targetMember.updateRole(newRole);
    }

    /**
     * 팀을 생성한다.
     *
     * <p>학교 인증된 사용자만 생성할 수 있다. 생성자를 {@link TeamRole#LEADER}로 첫 팀 멤버에 등록하고
     * 팀 채팅방을 함께 생성한다.</p>
     *
     * @param user 팀을 생성하는 사용자
     * @param request 생성할 팀 정보
     * @return 생성된 팀의 상세 정보
     * @throws RestApiException 사용자가 학교 인증되지 않은 경우
     */
    @Transactional
    public TeamDetailResponse createTeam(User user, TeamCreateRequest request) {

        if (!Boolean.TRUE.equals(user.getIsSchoolVerified())) {
            throw new RestApiException(CustomErrorCode.USER_SCHOOL_VERIFICATION_REQUIRED);
        }

        Team team = Team.create(request);
        teamRepository.save(team);

        // 팀 생성자를 Leader 로 TeamMember 에 추가
        TeamMember teamMember = TeamMember.create(team, user, TeamRole.LEADER);
        teamMemberRepository.save(teamMember);

        chatRoomService.createTeamChatRoom(team, user);

        String imageUrl = s3Service.getTeamImageUrl(team.getImageKey(), team.getCategory());

        // 생성 직후라 memberCount 1
        return TeamDetailResponse.create(team, teamMember, imageUrl, 1);
    }

    /**
     * 팀 정보를 수정한다.
     *
     * <p>{@link TeamRole#LEADER} 또는 {@link TeamRole#MANAGER}만 수정할 수 있다. 이미지가 변경되면
     * 기존 S3 이미지를 삭제하여 orphan 이미지가 남지 않도록 한다.</p>
     *
     * @param user 수정을 요청한 사용자
     * @param teamId 수정할 팀 ID
     * @param request 수정할 팀 정보
     * @return 수정된 팀의 상세 정보
     * @throws RestApiException 팀을 찾을 수 없거나, 사용자가 팀 멤버가 아니거나, 사용자가 일반 멤버인 경우
     */
    @Transactional
    public TeamDetailResponse updateTeam(User user, Long teamId, TeamUpdateRequest request) {

        Team team =
                teamRepository.findById(teamId).orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));

        TeamMember teamMember = teamMemberRepository
                .findByTeamAndUser(team, user)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));

        // LEADER, MANAGER 만 수정 가능
        if (teamMember.getTeamRole() == TeamRole.MEMBER) {
            throw new RestApiException(CustomErrorCode.TEAM_FORBIDDEN);
        }

        // 이미지가 변경된 경우 기존 S3 이미지 삭제 (orphan image 방지)
        String oldImageKey = team.getImageKey();
        team.update(request);

        String newImageKey = team.getImageKey();
        if (StringUtils.hasText(oldImageKey) && !oldImageKey.equals(newImageKey)) {
            s3Service.deleteImage(oldImageKey);
        }

        int memberCount = teamMemberRepository.countByTeam(team);
        String imageUrl = s3Service.getTeamImageUrl(newImageKey, team.getCategory());

        return TeamDetailResponse.create(team, teamMember, imageUrl, memberCount);
    }

    /**
     * 팀을 삭제한다.
     *
     * <p>팀 리더만 삭제할 수 있다. 모집글/신청서, 팀 공지(읽음 기록·이미지 포함), 팀 초대, 팀 멤버, 팀
     * 채팅방을 FK 제약이 해소되는 순서로 모두 삭제한 뒤 팀 이미지와 팀 자체를 삭제한다.</p>
     *
     * @param user 삭제를 요청한 사용자
     * @param teamId 삭제할 팀 ID
     * @throws RestApiException 팀을 찾을 수 없거나, 사용자가 팀 멤버가 아니거나, 사용자가 리더가 아닌 경우
     */
    @Transactional
    public void deleteTeam(User user, Long teamId) {

        Team team =
                teamRepository.findById(teamId).orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));

        TeamMember teamMember = teamMemberRepository
                .findByTeamAndUser(team, user)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));

        // LEADER 만 삭제 가능
        if (teamMember.getTeamRole() != TeamRole.LEADER) {
            throw new RestApiException(CustomErrorCode.TEAM_FORBIDDEN);
        }

        // 모집글 신청서 삭제 (RecruitmentApplication → Recruitment 순서로 FK 제약 해소)
        List<Recruitment> recruitments = recruitmentRepository.findAllByTeam(team);
        if (!recruitments.isEmpty()) {
            recruitmentApplicationRepository.deleteAllByRecruitmentIn(recruitments);
            recruitmentRepository.deleteAll(recruitments);
        }

        // 팀 공지 삭제 (읽음 기록 → 이미지 → 공지 순서로 FK 제약 해소)
        List<TeamNotice> notices = teamNoticeRepository.findAllByTeam(team);
        if (!notices.isEmpty()) {
            teamNoticeReadRepository.deleteAllByTeamNoticeIn(notices);
            teamNoticeImageRepository
                    .findAllByTeamNoticeIn(notices)
                    .forEach(img -> s3Service.deleteImage(img.getImageKey()));
            teamNoticeImageRepository.deleteAllByTeamNoticeIn(notices);
            teamNoticeRepository.deleteAll(notices);
        }

        // 팀 초대 삭제
        teamInvitationRepository.deleteAllByTeam(team);

        // 팀 멤버 삭제
        teamMemberRepository.deleteAllByTeam(team);

        // 팀 채팅방 삭제
        chatRoomService.deleteAllChatRoomsForTeam(team);

        if (StringUtils.hasText(team.getImageKey())) {
            s3Service.deleteImage(team.getImageKey());
        }

        teamRepository.delete(team);
    }

    /**
     * 팀에서 탈퇴한다.
     *
     * <p>리더는 팀 삭제 또는 리더 위임 후에만 탈퇴할 수 있다.</p>
     *
     * @param user 탈퇴할 사용자
     * @param teamId 탈퇴할 팀 ID
     * @throws RestApiException 팀을 찾을 수 없거나, 사용자가 팀 멤버가 아니거나, 사용자가 리더인 경우
     */
    @Transactional
    public void leaveTeam(User user, Long teamId) {
        Team team =
                teamRepository.findById(teamId).orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));

        TeamMember member = teamMemberRepository
                .findByTeamAndUser(team, user)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));

        // 리더는 탈퇴 불가 (팀 삭제 또는 리더 위임 후 가능)
        if (member.getTeamRole() == TeamRole.LEADER) {
            throw new RestApiException(CustomErrorCode.TEAM_MEMBER_IS_HOST);
        }

        removeMemberCascade(member);
    }

    /**
     * 팀원을 방출한다.
     *
     * <p>본인을 방출할 수 없고 리더는 방출할 수 없다. 일반 멤버는 방출 권한이 없으며, 매니저는 다른
     * 매니저를 방출할 수 없다.</p>
     *
     * @param user 방출을 요청한 사용자
     * @param teamId 대상 팀 ID
     * @param targetMemberId 방출할 팀 멤버 ID
     * @throws RestApiException 팀을 찾을 수 없거나, 요청자가 팀 멤버가 아니거나, 대상 멤버를 찾을 수 없거나,
     *                       대상 멤버가 해당 팀 소속이 아니거나, 본인을 방출하려 하거나, 대상이 리더이거나,
     *                       요청자가 일반 멤버이거나, 요청자와 대상이 모두 매니저인 경우
     */
    @Transactional
    public void kickMember(User user, Long teamId, Long targetMemberId) {
        Team team =
                teamRepository.findById(teamId).orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));

        TeamMember requester = teamMemberRepository
                .findByTeamAndUser(team, user)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));

        TeamMember target = teamMemberRepository
                .findById(targetMemberId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));

        if (!target.getTeam().getTeamId().equals(teamId)) {
            throw new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND);
        }

        if (target.getTeamMemberId().equals(requester.getTeamMemberId())) {
            throw new RestApiException(CustomErrorCode.TEAM_CANNOT_KICK_SELF);
        }

        if (target.getTeamRole() == TeamRole.LEADER) {
            throw new RestApiException(CustomErrorCode.TEAM_MEMBER_IS_HOST);
        }

        if (requester.getTeamRole() == TeamRole.MEMBER) {
            throw new RestApiException(CustomErrorCode.TEAM_FORBIDDEN);
        }

        if (requester.getTeamRole() == TeamRole.MANAGER && target.getTeamRole() == TeamRole.MANAGER) {
            throw new RestApiException(CustomErrorCode.TEAM_FORBIDDEN);
        }

        removeMemberCascade(target);
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 팀원 제거 시 연관된 투표/일정 참여 기록과 채팅방 멤버십을 함께 정리한다.
     *
     * <p>FK 제약 위반을 방지하기 위해 참여 기록을 먼저 삭제한 뒤 팀 멤버를 삭제한다.</p>
     *
     * @param member 제거할 팀 멤버
     */
    private void removeMemberCascade(TeamMember member) {
        voteAvailabilityRepository.deleteByVoteParticipant_TeamMember(member);
        voteParticipantRepository.deleteByTeamMember(member);
        recurrenceExceptionParticipantRepository.deleteByTeamMember(member);
        eventParticipantRepository.deleteByTeamMember(member);
        chatRoomService.removeTeamChatRoomMember(member.getTeam(), member.getUser());
        teamMemberRepository.delete(member);
    }
}
