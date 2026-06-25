package com.inuteamflow.server.domain.team.service;

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
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import com.inuteamflow.server.global.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    // 팀 리스트 조회 (내가 속한 팀)
    public List<TeamSummaryResponse> getMyTeams(User user) {

        List<TeamMember> memberships = teamMemberRepository.findByUserWithTeam(user);

        List<Team> teams = memberships.stream()
                .map(TeamMember::getTeam)
                .toList();

        Map<Long, Long> countMap = teamMemberRepository.countByTeams(teams)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        return memberships.stream()
                .map(tm -> {
                    Team team = tm.getTeam();
                    int memberCount = countMap.getOrDefault(team.getTeamId(), 0L).intValue();
                    String imageUrl = s3Service.getImageUrl(team.getImageKey());
                    return TeamSummaryResponse.create(team, imageUrl, memberCount);
                })
                .toList();
    }

    // 팀 상세 조회
    public TeamDetailResponse getTeamDetails(Long teamId, User user) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));

        // 현재 로그인한 유저가 해당 팀의 멤버인지 확인
        TeamMember teamMember = teamMemberRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));

        int memberCount = teamMemberRepository.countByTeam(team);
        String imageUrl = s3Service.getImageUrl(team.getImageKey());

        return TeamDetailResponse.create(team, teamMember, imageUrl, memberCount);
    }

    // 팀 멤버 조회
    public List<TeamMemberResponse> getTeamMembers(Long teamId, User user) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));

        teamMemberRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));

        return teamMemberRepository.findByTeam(team).stream()
                .map(tm -> TeamMemberResponse.create(tm, tm.getUser()))
                .toList();
    }

    // 팀 매니저 지정, 해제
    @Transactional
    public void updateMemberRole(User user, Long teamId, Long targetMemberId, TeamRole newRole) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));

        TeamMember requester = teamMemberRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));

        if (requester.getTeamRole() != TeamRole.LEADER) {
            throw new RestApiException(CustomErrorCode.TEAM_FORBIDDEN);
        }

        TeamMember targetMember = teamMemberRepository.findById(targetMemberId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));

        if (!targetMember.getTeam().getTeamId().equals(teamId)) {
            throw new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND);
        }

        // 이미 같은 역할
        if (targetMember.getTeamRole() == newRole) {
            throw new RestApiException(CustomErrorCode.TEAM_MEMBER_ALREADY_ROLE);
        }

        targetMember.updateRole(newRole);
    }

    // 팀 생성
    @Transactional
    public TeamDetailResponse createTeam(User user, TeamCreateRequest request) {

        Team team = Team.create(request);
        teamRepository.save(team);

        // 팀 생성자를 Leader 로 TeamMember 에 추가
        TeamMember teamMember = TeamMember.create(team, user, TeamRole.LEADER);
        teamMemberRepository.save(teamMember);

        String imageUrl = s3Service.getImageUrl(team.getImageKey());

        // 생성 직후라 memberCount 1
        return TeamDetailResponse.create(team, teamMember, imageUrl, 1);
    }

    // 팀 수정
    @Transactional
    public TeamDetailResponse updateTeam(User user, Long teamId, TeamUpdateRequest request) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));

        TeamMember teamMember = teamMemberRepository.findByTeamAndUser(team, user)
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
        String imageUrl = s3Service.getImageUrl(newImageKey);

        return TeamDetailResponse.create(team, teamMember, imageUrl, memberCount);

    }

    // 팀 삭제
    @Transactional
    public void deleteTeam(User user, Long teamId) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));

        TeamMember teamMember = teamMemberRepository.findByTeamAndUser(team, user)
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

        // 팀 초대 삭제
        teamInvitationRepository.deleteAllByTeam(team);

        // 팀 멤버 삭제
        teamMemberRepository.deleteAllByTeam(team);

        if (StringUtils.hasText(team.getImageKey())) {
            s3Service.deleteImage(team.getImageKey());
        }

        teamRepository.delete(team);
    }

}
