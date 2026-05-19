package com.inuteamflow.server.domain.team.service;

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
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import com.inuteamflow.server.global.s3.PresignedUrlRequest;
import com.inuteamflow.server.global.s3.PresignedUrlResponse;
import com.inuteamflow.server.global.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final S3Service s3Service;
    private final UserRepository userRepository;

    // 팀 리스트 조회 (내가 속한 팀)
    public List<TeamSummaryResponse> getMyTeams(UserDetailsImpl userDetails) {

        List<TeamMember> myMemberships = teamMemberRepository.findByUser(userDetails.getUser());

        return myMemberships.stream()
                .map(tm -> {
                    Team team = tm.getTeam();
                    int memberCount = teamMemberRepository.countByTeam(team);
                    String imageUrl = s3Service.getImageUrl(team.getImageKey());
                    return TeamSummaryResponse.create(team, imageUrl, memberCount);
                })
                .toList();
    }

    // 팀 상세 조회
    public TeamDetailResponse getTeamDetails(Long teamId, UserDetailsImpl userDetails) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));

        // 현재 로그인한 유저가 해당 팀의 멤버인지 확인
        TeamMember teamMember = teamMemberRepository.findByTeamAndUser(team, userDetails.getUser())
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));

        int memberCount = teamMemberRepository.countByTeam(team);
        String imageUrl = s3Service.getImageUrl(team.getImageKey());

        return TeamDetailResponse.create(team, teamMember, imageUrl, memberCount);
    }

    // 팀 멤버 조회
    public List<TeamMemberResponse> getTeamMembers(Long teamId, UserDetailsImpl userDetails) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));

        teamMemberRepository.findByTeamAndUser(team, userDetails.getUser())
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));

        return teamMemberRepository.findByTeam(team).stream()
                .map(tm -> TeamMemberResponse.create(tm, tm.getUser()))
                .toList();
    }

    // 팀 매니저 지정, 해제
    @Transactional
    public void updateMemberRole(UserDetailsImpl userDetails, Long teamId, Long targetMemberId, TeamRole newRole) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));

        TeamMember requester = teamMemberRepository.findByTeamAndUser(team, userDetails.getUser())
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
    public TeamDetailResponse createTeam(UserDetailsImpl userDetails, TeamCreateRequest request) {

        User creator = userDetails.getUser();

        Team team = Team.create(request);
        teamRepository.save(team);

        // 팀 생성자를 Leader 로 TeamMember 에 추가
        TeamMember teamMember = TeamMember.create(team, creator, TeamRole.LEADER);
        teamMemberRepository.save(teamMember);

        String imageUrl = s3Service.getImageUrl(team.getImageKey());

        // 생성 직후라 memberCount 1
        return TeamDetailResponse.create(team, teamMember, imageUrl, 1);
    }

    // 팀 수정
    @Transactional
    public TeamDetailResponse updateTeam(UserDetailsImpl userDetails, Long teamId, TeamUpdateRequest request) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));

        TeamMember teamMember = teamMemberRepository.findByTeamAndUser(team, userDetails.getUser())
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
    public void deleteTeam(UserDetailsImpl userDetails, Long teamId) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));

        TeamMember teamMember = teamMemberRepository.findByTeamAndUser(team, userDetails.getUser())
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));

        // LEADER 만 삭제 가능
        if (teamMember.getTeamRole() != TeamRole.LEADER) {
            throw new RestApiException(CustomErrorCode.TEAM_FORBIDDEN);
        }

        // 팀 멤버 먼저 삭제 (FK 제약 때문에 Team 보다 먼저)
        teamMemberRepository.deleteAllByTeam(team);

        if (StringUtils.hasText(team.getImageKey())) {
            s3Service.deleteImage(team.getImageKey());
        }

        teamRepository.delete(team);
    }

    // 팀 프로필 Presigned URL 요청
    public PresignedUrlResponse getPresignedUrl(UserDetailsImpl userDetails, Long teamId, PresignedUrlRequest request) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));

        TeamMember teamMember = teamMemberRepository.findByTeamAndUser(team, userDetails.getUser())
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));

        if (teamMember.getTeamRole() == TeamRole.MEMBER) {
            throw new RestApiException(CustomErrorCode.TEAM_FORBIDDEN);
        }

        return s3Service.getTeamBannerPresignedUrl(request);
    }
}
