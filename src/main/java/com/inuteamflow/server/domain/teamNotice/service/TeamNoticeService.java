package com.inuteamflow.server.domain.teamNotice.service;

import com.inuteamflow.server.domain.notification.enums.NotificationType;
import com.inuteamflow.server.domain.notification.service.NotificationService;
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
	private final NotificationService notificationService;

	// =========================================================================
	// ============================= 주요 서비스 기능 =============================
	// =========================================================================

	/**
	 * 팀에 등록된 공지 목록을 조회한다.
	 *
	 * <p>각 공지에는 요청자의 읽음 여부와 작성자의 이름 및 팀 역할이 포함된다.</p>
	 *
	 * @param teamId 공지를 조회할 팀 ID
	 * @param user 공지 목록을 조회하는 사용자
	 * @param pageable 페이지 요청 정보
	 * @return 팀 공지 요약 정보 페이지
	 * @throws RestApiException 팀을 찾을 수 없거나 사용자가 해당 팀의 멤버가 아닌 경우
	 */
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

	/**
	 * 사용자가 속한 모든 팀의 공지 목록을 조회한다.
	 *
	 * <p>소속 팀이 없는 경우 빈 페이지를 반환하며, 각 공지에는 요청자의 읽음 여부와
	 * 작성자의 이름 및 팀 역할이 포함된다.</p>
	 *
	 * @param user 공지 목록을 조회하는 사용자
	 * @param pageable 페이지 요청 정보
	 * @return 사용자가 속한 팀의 공지 요약 정보 페이지
	 */
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

	/**
	 * 사용자가 작성한 공지 목록을 조회한다.
	 *
	 * <p>각 공지에는 요청자의 읽음 여부와 해당 팀에서의 역할이 포함된다.</p>
	 *
	 * @param user 공지를 작성한 사용자
	 * @param pageable 페이지 요청 정보
	 * @return 사용자가 작성한 공지 요약 정보 페이지
	 */
	public Page<TeamNoticeSummaryResponse> getMyNotices(User user, Pageable pageable) {
		Page<TeamNotice> noticePage = teamNoticeRepository.findByCreatedBy(user.getUserId(), pageable);
		List<TeamNotice> notices = noticePage.getContent();

		Set<Long> readNoticeIds = notices.isEmpty() ? Set.of()
				: teamNoticeReadRepository.findReadNoticeIds(notices, user.getUserId());
		Map<Long, TeamMember> memberByTeamId = teamMemberRepository.findByUserWithTeam(user).stream()
				.collect(Collectors.toMap(
						tm -> tm.getTeam().getTeamId(),
						Function.identity()
				));

		return noticePage.map(notice -> {
			boolean isRead = readNoticeIds.contains(notice.getTeamNoticeId());
			TeamMember authorMember = memberByTeamId.get(notice.getTeam().getTeamId());
			return TeamNoticeSummaryResponse.create(notice, isRead, user.getName(), authorMember.getTeamRole());
		});
	}

	/**
	 * 팀 공지의 상세 정보를 조회하고 읽음 처리한다.
	 *
	 * @param teamId 공지가 속한 팀 ID
	 * @param noticeId 조회할 공지 ID
	 * @param user 공지를 조회하는 사용자
	 * @return 공지 내용과 작성자 및 이미지 정보
	 * @throws RestApiException 팀, 팀 멤버, 공지 또는 공지 작성자를 찾을 수 없는 경우
	 */
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

	/**
	 * 팀에 공지를 생성한다.
	 *
	 * <p>첨부 이미지를 함께 저장하고 작성자를 제외한 팀 멤버에게 알림을 전송한다.</p>
	 *
	 * @param teamId 공지를 생성할 팀 ID
	 * @param user 공지를 작성하는 사용자
	 * @param request 공지 내용과 첨부 이미지 정보
	 * @return 생성된 공지 상세 정보
	 * @throws RestApiException 팀을 찾을 수 없거나 사용자가 해당 팀의 멤버가 아닌 경우
	 */
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

		// 팀 멤버의 유저 객체들을 한 번에 조회
		List<User> receivers = teamMemberRepository.findUsersByTeamExcluding(team, user.getUserId());

		notificationService.createNotifications(
				receivers,
				"[" + team.getName() + "] 팀에 새 공지가 올라왔어요",
				notice.getTitle(),
				NotificationType.NOTICE,
				"/team/" + team.getTeamId() + "/notice/" + notice.getTeamNoticeId()
		);

		return TeamNoticeDetailResponse.create(notice, member, authorProfileUrl, images, s3Service::getImageUrl, true);
	}

	/**
	 * 팀 공지의 내용과 첨부 이미지를 수정한다.
	 *
	 * <p>기존 첨부 이미지를 모두 삭제한 후 요청된 이미지로 다시 저장한다.</p>
	 *
	 * @param teamId 공지가 속한 팀 ID
	 * @param noticeId 수정할 공지 ID
	 * @param user 공지를 수정하는 사용자
	 * @param request 수정할 공지 내용과 첨부 이미지 정보
	 * @return 수정된 공지 상세 정보
	 * @throws RestApiException 팀, 팀 멤버, 공지 또는 공지 작성자를 찾을 수 없는 경우,
	 *                          또는 공지 수정 권한이 없는 경우
	 */
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

	/**
	 * 팀 공지와 관련 데이터를 삭제한다.
	 *
	 * <p>공지의 읽음 기록과 첨부 이미지 정보를 삭제하고 저장소의 이미지도 제거한다.</p>
	 *
	 * @param teamId 공지가 속한 팀 ID
	 * @param noticeId 삭제할 공지 ID
	 * @param user 공지를 삭제하는 사용자
	 * @throws RestApiException 팀, 팀 멤버 또는 공지를 찾을 수 없는 경우,
	 *                          또는 공지 삭제 권한이 없는 경우
	 */
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

	 // =========================================================================
	 // ================================ 헬퍼 함수 ================================
	 // =========================================================================

	/**
	 * 공지에 첨부할 이미지를 순서대로 저장한다.
	 *
	 * @param notice 이미지가 속한 공지
	 * @param imageKeys 저장할 이미지 키 목록
	 * @return 저장된 공지 이미지 목록
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

	/**
	 * 팀 멤버가 공지를 수정하거나 삭제할 수 있는지 확인한다.
	 *
	 * @param notice 권한을 확인할 공지
	 * @param member 권한을 확인할 팀 멤버
	 * @return 공지 작성자이거나 팀장 또는 매니저이면 {@code true}, 그렇지 않으면 {@code false}
	 */
	private boolean isEditable(TeamNotice notice, TeamMember member) {
		return notice.getCreatedBy().equals(member.getUser().getUserId())
				|| member.getTeamRole() == TeamRole.LEADER
				|| member.getTeamRole() == TeamRole.MANAGER;
	}

	/**
	 * 팀 멤버의 공지 수정 및 삭제 권한을 검증한다.
	 *
	 * @param notice 권한을 검증할 공지
	 * @param member 권한을 검증할 팀 멤버
	 * @throws RestApiException 공지 수정 또는 삭제 권한이 없는 경우
	 */
	private void validateEditable(TeamNotice notice, TeamMember member) {
		if (!isEditable(notice, member)) {
			throw new RestApiException(CustomErrorCode.TEAM_NOTICE_FORBIDDEN);
		}
	}

	/**
	 * 팀에서 공지 작성자를 조회한다.
	 *
	 * @param team 공지가 속한 팀
	 * @param userId 공지 작성자 ID
	 * @return 공지 작성자의 팀 멤버 정보
	 * @throws RestApiException 공지 작성자를 팀 멤버에서 찾을 수 없는 경우
	 */
	private TeamMember findAuthorMember(Team team, Long userId) {
		return teamMemberRepository.findByTeamAndUserUserId(team, userId)
				.orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));
	}

	/**
	 * ID로 팀을 조회한다.
	 *
	 * @param teamId 조회할 팀 ID
	 * @return 조회된 팀
	 * @throws RestApiException 팀을 찾을 수 없는 경우
	 */
	private Team getTeamById(Long teamId) {
		return teamRepository.findById(teamId)
				.orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));
	}

	/**
	 * 팀에 속한 사용자의 멤버 정보를 조회한다.
	 *
	 * @param team 멤버가 속한 팀
	 * @param user 조회할 사용자
	 * @return 사용자의 팀 멤버 정보
	 * @throws RestApiException 사용자가 해당 팀의 멤버가 아닌 경우
	 */
	private TeamMember getTeamMember(Team team, User user) {
		return teamMemberRepository.findByTeamAndUser(team, user)
				.orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));
	}

	/**
	 * 팀에 속한 공지를 조회한다.
	 *
	 * @param noticeId 조회할 공지 ID
	 * @param team 공지가 속한 팀
	 * @return 조회된 팀 공지
	 * @throws RestApiException 해당 팀에서 공지를 찾을 수 없는 경우
	 */
	private TeamNotice getNoticeByIdAndTeam(Long noticeId, Team team) {
		return teamNoticeRepository.findByTeamNoticeIdAndTeam(noticeId, team)
				.orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOTICE_NOT_FOUND));
	}
}
