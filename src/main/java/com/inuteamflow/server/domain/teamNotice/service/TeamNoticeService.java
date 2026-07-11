package com.inuteamflow.server.domain.teamNotice.service;

import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.domain.team.enums.TeamRole;
import com.inuteamflow.server.domain.team.repository.TeamMemberRepository;
import com.inuteamflow.server.domain.team.repository.TeamRepository;
import com.inuteamflow.server.domain.teamNotice.dto.req.TeamNoticeCreateRequest;
import com.inuteamflow.server.domain.teamNotice.dto.req.TeamNoticeUpdateRequest;
import com.inuteamflow.server.domain.teamNotice.dto.res.TeamNoticeDetailResponse;
import com.inuteamflow.server.domain.teamNotice.dto.res.TeamNoticeSummaryResponse;
import com.inuteamflow.server.domain.teamNotice.entity.TeamNotice;
import com.inuteamflow.server.domain.teamNotice.entity.TeamNoticeImage;
import com.inuteamflow.server.domain.teamNotice.entity.TeamNoticeRead;
import com.inuteamflow.server.domain.teamNotice.repository.TeamNoticeImageRepository;
import com.inuteamflow.server.domain.teamNotice.repository.TeamNoticeReadRepository;
import com.inuteamflow.server.domain.teamNotice.repository.TeamNoticeRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import com.inuteamflow.server.global.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamNoticeService {

	private final TeamNoticeRepository teamNoticeRepository;
	private final TeamNoticeReadRepository teamNoticeReadRepository;
	private final TeamNoticeImageRepository teamNoticeImageRepository;
	private final TeamRepository teamRepository;
	private final TeamMemberRepository teamMemberRepository;
	private final S3Service s3Service;

	public Page<TeamNoticeSummaryResponse> getTeamNotices(Long teamId, User user, Pageable pageable) {
		Team team = getTeamById(teamId);
		getTeamMember(team, user);

		// TeamNotice 엔티티를 Page로 조회
		Page<TeamNotice> noticePage = teamNoticeRepository.findByTeam(team, pageable);
		List<TeamNotice> notices = noticePage.getContent();

		// N+1 방지: 읽음 여부와 작성자 정보를 각각 1번의 쿼리로 일괄 조회
		Set<Long> readNoticeIds = notices.isEmpty() ? Set.of()
				: teamNoticeReadRepository.findReadNoticeIds(notices, user.getUserId());
		Map<Long, TeamMember> memberByUserId = teamMemberRepository.findByTeamWithUser(team).stream()
				.collect(Collectors.toMap(tm -> tm.getUser().getUserId(), Function.identity()));

		return noticePage.map(notice -> {
			boolean isRead = readNoticeIds.contains(notice.getTeamNoticeId());
			TeamMember authorMember = memberByUserId.get(notice.getCreatedBy());
			return TeamNoticeSummaryResponse.create(notice, isRead, authorMember.getUser().getName(), authorMember.getTeamRole());
		});
	}

	public Page<TeamNoticeSummaryResponse> getMyTeamNotices(User user, Pageable pageable) {
		// 로그인 한 유저가 속한 팀을 List로 조회
		// findByUserWithTeam: team LAZY 로딩 방지
		List<Team> myTeams = teamMemberRepository.findByUserWithTeam(user).stream()
				.map(TeamMember::getTeam)
				.toList();

		if (myTeams.isEmpty()) {
			return Page.empty(pageable);
		}

		// TeamNotice 엔티티를 Page로 조회
		Page<TeamNotice> noticePage = teamNoticeRepository.findByTeamIn(myTeams, pageable);
		List<TeamNotice> notices = noticePage.getContent();

		// N+1 방지: 여러 팀에 걸친 읽음 여부와 작성자 정보를 각각 1번의 쿼리로 일괄 조회
		Set<Long> readNoticeIds = notices.isEmpty() ? Set.of()
				: teamNoticeReadRepository.findReadNoticeIds(notices, user.getUserId());
		Map<Long, Map<Long, TeamMember>> membersByTeamId = teamMemberRepository.findByTeamInWithUser(myTeams).stream()
				.collect(Collectors.groupingBy(
						tm -> tm.getTeam().getTeamId(),
						Collectors.toMap(tm -> tm.getUser().getUserId(), Function.identity())
				));

		return noticePage.map(notice -> {
			boolean isRead = readNoticeIds.contains(notice.getTeamNoticeId());
			TeamMember authorMember = membersByTeamId
					.getOrDefault(notice.getTeam().getTeamId(), Map.of())
					.get(notice.getCreatedBy());
			return TeamNoticeSummaryResponse.create(notice, isRead, authorMember.getUser().getName(), authorMember.getTeamRole());
		});
	}

	@Transactional
	public TeamNoticeDetailResponse getTeamNotice(Long teamId, Long noticeId, User user) {
		Team team = getTeamById(teamId);
		TeamMember member = getTeamMember(team, user);
		TeamNotice notice = getNoticeByIdAndTeam(noticeId, team);

		// 상세 조회 시 읽음 처리
		if (!teamNoticeReadRepository.existsByTeamNoticeAndCreatedBy(notice, user.getUserId())) {
			teamNoticeReadRepository.save(TeamNoticeRead.create(notice));
		}

		// 이미지를 순서에 맞게 정렬하여 List로 조회
		// 작성자 정보를 조회
		List<TeamNoticeImage> images = teamNoticeImageRepository.findByTeamNoticeOrderBySortOrderAsc(notice);
		TeamMember authorMember = findAuthorMember(team, notice.getCreatedBy());
		String authorProfileUrl = s3Service.getImageUrl(authorMember.getUser().getImageKey());
		boolean isEditable = isEditable(notice, member);

		return TeamNoticeDetailResponse.create(notice, authorMember, authorProfileUrl, images, s3Service::getImageUrl, isEditable);
	}

	@Transactional
	public TeamNoticeDetailResponse createTeamNotice(Long teamId, User user, TeamNoticeCreateRequest request) {
		Team team = getTeamById(teamId);
		TeamMember member = getTeamMember(team, user);

		// TeamNotice 생성
		TeamNotice notice = TeamNotice.create(team, request);
		teamNoticeRepository.save(notice);

		// TeamNoticeImage 생성
		List<TeamNoticeImage> images = saveImages(notice, request.getImageKeys());
		String authorProfileUrl = s3Service.getImageUrl(user.getImageKey());

		return TeamNoticeDetailResponse.create(notice, member, authorProfileUrl, images, s3Service::getImageUrl, true);
	}

	@Transactional
	public TeamNoticeDetailResponse updateTeamNotice(Long teamId, Long noticeId, User user, TeamNoticeUpdateRequest request) {
		Team team = getTeamById(teamId);
		TeamMember member = getTeamMember(team, user);
		TeamNotice notice = getNoticeByIdAndTeam(noticeId, team);
		validateEditable(notice, member);

		// 기존 이미지를 DB 에서 제거하기 전에 S3 키를 수집해두어야 이후 삭제 가능
		// 기존 이미지 전체를 삭제하고 새롭게 요청으로 들어온 이미지를 저장
		List<TeamNoticeImage> oldImages = teamNoticeImageRepository.findByTeamNoticeOrderBySortOrderAsc(notice);
		notice.update(request);
		teamNoticeImageRepository.deleteByTeamNotice(notice);
		oldImages.forEach(img -> s3Service.deleteImage(img.getImageKey()));
		List<TeamNoticeImage> images = saveImages(notice, request.getImageKeys());

		TeamMember authorMember = findAuthorMember(team, notice.getCreatedBy());
		String authorProfileUrl = s3Service.getImageUrl(authorMember.getUser().getImageKey());
		boolean isEditable = isEditable(notice, member);

		return TeamNoticeDetailResponse.create(notice, authorMember, authorProfileUrl, images, s3Service::getImageUrl, isEditable);
	}

	@Transactional
	public void deleteTeamNotice(Long teamId, Long noticeId, User user) {
		Team team = getTeamById(teamId);
		TeamMember member = getTeamMember(team, user);
		TeamNotice notice = getNoticeByIdAndTeam(noticeId, team);
		validateEditable(notice, member);

		// DB 삭제 전에 S3 키를 수집해두어야 이후 삭제 가능
		List<TeamNoticeImage> images = teamNoticeImageRepository.findByTeamNoticeOrderBySortOrderAsc(notice);
		teamNoticeImageRepository.deleteByTeamNotice(notice);
		teamNoticeReadRepository.deleteByTeamNotice(notice);
		teamNoticeRepository.delete(notice);
		images.forEach(img -> s3Service.deleteImage(img.getImageKey()));
	}

	/**
	 * =========================================================================
	 * ================================ 헬퍼 함수 ================================
	 * =========================================================================
	 */

	private List<TeamNoticeImage> saveImages(TeamNotice notice, List<String> imageKeys) {
		if (imageKeys == null || imageKeys.isEmpty()) {
			return List.of();
		}

		List<TeamNoticeImage> images = new ArrayList<>();
		for (int i = 0; i < imageKeys.size(); i++) {
			images.add(TeamNoticeImage.create(imageKeys.get(i), i, notice));
		}
		return teamNoticeImageRepository.saveAll(images);
	}

	private boolean isEditable(TeamNotice notice, TeamMember member) {
		return notice.getCreatedBy().equals(member.getUser().getUserId())
				|| member.getTeamRole() == TeamRole.LEADER
				|| member.getTeamRole() == TeamRole.MANAGER;
	}

	private void validateEditable(TeamNotice notice, TeamMember member) {
		if (!isEditable(notice, member)) {
			throw new RestApiException(CustomErrorCode.TEAM_NOTICE_FORBIDDEN);
		}
	}

	private TeamMember findAuthorMember(Team team, Long userId) {
		return teamMemberRepository.findByTeamAndUserUserId(team, userId)
				.orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));
	}

	private Team getTeamById(Long teamId) {
		return teamRepository.findById(teamId)
				.orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));
	}

	private TeamMember getTeamMember(Team team, User user) {
		return teamMemberRepository.findByTeamAndUser(team, user)
				.orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));
	}

	private TeamNotice getNoticeByIdAndTeam(Long noticeId, Team team) {
		return teamNoticeRepository.findByTeamNoticeIdAndTeam(noticeId, team)
				.orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOTICE_NOT_FOUND));
	}
}