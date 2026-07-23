package com.inuteamflow.server.domain.teamNotice;

import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.domain.team.enums.TeamRole;
import com.inuteamflow.server.domain.team.repository.TeamMemberRepository;
import com.inuteamflow.server.domain.team.repository.TeamRepository;
import com.inuteamflow.server.domain.teamNotice.dto.res.TeamNoticeSummaryResponse;
import com.inuteamflow.server.domain.teamNotice.entity.TeamNotice;
import com.inuteamflow.server.domain.teamNotice.repository.TeamNoticeRepository;
import com.inuteamflow.server.domain.teamNotice.service.TeamNoticeService;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.domain.user.enums.Department;
import com.inuteamflow.server.domain.user.enums.Role;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.global.enums.Category;
import com.inuteamflow.server.global.s3.S3Service;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TeamNoticeService#getMyNotices(User, org.springframework.data.domain.Pageable)}의
 * 작성자별 공지 조회와 페이지네이션을 실제 Repository 및 H2 데이터베이스로 검증한다.
 * - 로그인 사용자가 작성한 공지만 조회되고 다른 사용자의 공지는 제외되는지 확인한다.
 * - 조회 결과의 페이지 크기, 전체 요소 수, 전체 페이지 수 등 페이지 정보를 확인한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TeamNoticeServiceTest {

	@Autowired private TeamNoticeService teamNoticeService;
	@Autowired private UserRepository userRepository;
	@Autowired private TeamRepository teamRepository;
	@Autowired private TeamMemberRepository teamMemberRepository;
	@Autowired private TeamNoticeRepository teamNoticeRepository;
	@Autowired private EntityManager entityManager;

	@MockitoBean private S3Service s3Service;

	private User author;

	@BeforeEach
	void setUp() {
		author = createUser("author", "author@inu.ac.kr");
		User other = createUser("other", "other@inu.ac.kr");

		actingAs(author);
		Team team = teamRepository.saveAndFlush(Team.builder()
				.name("테스트 팀")
				.description("테스트 팀 설명")
				.category(Category.PROJECT)
				.build());

		teamMemberRepository.save(TeamMember.create(team, author, TeamRole.LEADER));
		teamMemberRepository.save(TeamMember.create(team, other, TeamRole.MEMBER));

		LocalDateTime baseTime = LocalDateTime.of(2026, 7, 23, 10, 0);

		actingAs(author);
		TeamNotice myFirstNotice = saveNotice(team, "내가 작성한 공지 1", true);
		TeamNotice mySecondNotice = saveNotice(team, "내가 작성한 공지 2", false);

		actingAs(other);
		TeamNotice otherFirstNotice = saveNotice(team, "다른 사용자가 작성한 공지 1", false);
		TeamNotice otherSecondNotice = saveNotice(team, "다른 사용자가 작성한 공지 2", false);

		entityManager.flush();
		// 생성 시각을 명확히 구분하여 공지의 순서가 보장되지 않는 문제를 해결
		updateCreatedAt(myFirstNotice, baseTime);
		updateCreatedAt(mySecondNotice, baseTime.plusMinutes(1));
		updateCreatedAt(otherFirstNotice, baseTime.plusMinutes(2));
		updateCreatedAt(otherSecondNotice, baseTime.plusMinutes(3));
		entityManager.clear();
	}

	@Test
	@DisplayName("내가 작성한 공지만 조회한다")
	void getMyNotices_returnsOnlyNoticesCreatedByMe() {
		Page<TeamNoticeSummaryResponse> result = teamNoticeService.getMyNotices(
				author,
				PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
		);

		assertThat(result.getContent())
				.extracting(TeamNoticeSummaryResponse::getAuthorName)
				.containsOnly(author.getName());
		assertThat(result.getContent())
				.extracting(TeamNoticeSummaryResponse::getTitle)
				.containsExactly(
						"내가 작성한 공지 2",
						"내가 작성한 공지 1"
				);
		assertThat(result.getContent())
				.extracting(TeamNoticeSummaryResponse::getCreatedAt)
				.isSortedAccordingTo((first, second) -> second.compareTo(first));
	}

	@Test
	@DisplayName("내가 속한 모든 팀의 공지를 최신순으로 조회한다")
	void getMyTeamNotices_returnsNoticesInLatestOrder() {
		Page<TeamNoticeSummaryResponse> result = teamNoticeService.getMyTeamNotices(
				author,
				PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
		);

		assertThat(result.getContent())
				.extracting(TeamNoticeSummaryResponse::getTitle)
				.containsExactly(
						"다른 사용자가 작성한 공지 2",
						"다른 사용자가 작성한 공지 1",
						"내가 작성한 공지 2",
						"내가 작성한 공지 1"
				);
		assertThat(result.getContent())
				.extracting(TeamNoticeSummaryResponse::getCreatedAt)
				.isSortedAccordingTo((first, second) -> second.compareTo(first));
	}

	@Test
	@DisplayName("내가 작성한 공지 목록의 페이지 정보를 반환한다")
	void getMyNotices_returnsPaginationMetadata() {
		Page<TeamNoticeSummaryResponse> result = teamNoticeService.getMyNotices(
				author,
				PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"))
		);

		assertThat(result.getNumber()).isZero();
		assertThat(result.getSize()).isEqualTo(1);
		assertThat(result.getNumberOfElements()).isEqualTo(1);
		assertThat(result.getTotalElements()).isEqualTo(2);
		assertThat(result.getTotalPages()).isEqualTo(2);
		assertThat(result.isFirst()).isTrue();
		assertThat(result.isLast()).isFalse();
		assertThat(result.getContent().get(0).getTitle()).isEqualTo("내가 작성한 공지 2");
	}

	private User createUser(String username, String email) {
		return userRepository.save(User.builder()
				.username(username)
				.email(email)
				.password("encoded-password")
				.name(username)
				.department(Department.COMPUTER_SCIENCE)
				.isSchoolVerified(false)
				.role(Role.USER)
				.build());
	}

	private TeamNotice saveNotice(Team team, String title, boolean isPinned) {
		return teamNoticeRepository.saveAndFlush(TeamNotice.builder()
				.team(team)
				.title(title)
				.content(title + " 내용")
				.isPinned(isPinned)
				.build());
	}

	private void updateCreatedAt(TeamNotice notice, LocalDateTime createdAt) {
		entityManager.createNativeQuery("""
						UPDATE team_notice
						SET created_at = :createdAt
						WHERE team_notice_id = :noticeId
						""")
				.setParameter("createdAt", createdAt)
				.setParameter("noticeId", notice.getTeamNoticeId())
				.executeUpdate();
	}

	private void actingAs(User user) {
		UserDetailsImpl userDetails = new UserDetailsImpl(user);
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(
						userDetails,
						null,
						userDetails.getAuthorities()
				)
		);
	}
}
