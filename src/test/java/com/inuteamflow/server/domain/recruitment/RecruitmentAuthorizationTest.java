package com.inuteamflow.server.domain.recruitment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.inuteamflow.server.domain.notification.service.NotificationService;
import com.inuteamflow.server.domain.recruitment.dto.request.ApplicationStatusUpdateRequest;
import com.inuteamflow.server.domain.recruitment.dto.request.RecruitmentCreateRequest;
import com.inuteamflow.server.domain.recruitment.entity.Recruitment;
import com.inuteamflow.server.domain.recruitment.entity.RecruitmentApplication;
import com.inuteamflow.server.domain.recruitment.repository.RecruitmentApplicationRepository;
import com.inuteamflow.server.domain.recruitment.repository.RecruitmentRepository;
import com.inuteamflow.server.domain.recruitment.service.RecruitmentApplicationService;
import com.inuteamflow.server.domain.recruitment.service.RecruitmentService;
import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.domain.team.enums.TeamRole;
import com.inuteamflow.server.domain.team.repository.TeamMemberRepository;
import com.inuteamflow.server.domain.team.repository.TeamRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.domain.user.enums.Department;
import com.inuteamflow.server.domain.user.enums.Role;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.global.enums.Category;
import com.inuteamflow.server.global.enums.Status;
import com.inuteamflow.server.global.exception.error.RestApiException;
import com.inuteamflow.server.global.s3.S3Service;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * 채용(모집) 도메인의 팀 권한 검증 취약점(이슈 #94)을 실제 Repository와 H2 데이터베이스로 검증한다.
 *
 * <p>공격 흐름: 공격자가 피해 팀 ID로 모집글을 생성 → 다른 계정으로 지원 → 공격자가 지원 승인 → 피해 팀에 무단 가입.
 * 공고 생성과 지원 승인 모두 대상 팀에 대한 요청자 역할을 재검증해야 하므로, 두 지점을 각각 독립적으로 검증한다.</p>
 *
 * <p>담당 개발자의 프로덕션 수정 대기 상태이며, 수정 전에는 두 테스트 모두 실패(red)한다.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecruitmentAuthorizationTest {

    @Autowired
    private RecruitmentService recruitmentService;

    @Autowired
    private RecruitmentApplicationService recruitmentApplicationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private RecruitmentRepository recruitmentRepository;

    @Autowired
    private RecruitmentApplicationRepository recruitmentApplicationRepository;

    @MockitoBean
    private S3Service s3Service;

    @MockitoBean
    private NotificationService notificationService;

    private User attacker;
    private User applicant;
    private Team team;

    @BeforeEach
    void setUp() {
        User teamLeader = createUser("team-leader", "team-leader@inu.ac.kr");
        attacker = createUser("attacker", "attacker@inu.ac.kr");
        applicant = createUser("applicant", "applicant@inu.ac.kr");

        // 공격자와 무관한 리더가 소유한 팀 (공격자는 이 팀의 멤버가 아니다)
        actingAs(teamLeader);
        team = teamRepository.save(Team.builder()
                .name("테스트 팀")
                .description("테스트 팀 설명")
                .category(Category.PROJECT)
                .build());
        teamMemberRepository.save(TeamMember.create(team, teamLeader, TeamRole.LEADER));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("피해 팀에 대한 권한이 없는 사용자는 그 팀으로 모집글을 생성할 수 없다")
    void createRecruitment_byUserWithoutTeamAuthority_isRejected() {
        // given: 공격자가 피해 팀 ID로 모집글 생성을 요청
        RecruitmentCreateRequest request = mock(RecruitmentCreateRequest.class);
        when(request.getTeamId()).thenReturn(team.getTeamId());
        when(request.getTitle()).thenReturn("무단 모집글");
        when(request.getCategory()).thenReturn(Category.PROJECT);
        when(request.getDescription()).thenReturn("피해 팀을 사칭한 모집글");
        when(request.getTargetMemberCount()).thenReturn(5);
        when(request.getEndAt()).thenReturn(LocalDateTime.now().plusDays(7));
        when(request.getInfoPostId()).thenReturn(null);

        // when & then: 팀 멤버(리더)가 아니므로 권한 오류로 거부되어야 한다 (RECRUITMENT_FORBIDDEN / TEAM_FORBIDDEN)
        actingAs(attacker);
        assertThatThrownBy(() -> recruitmentService.createRecruitment(request, attacker))
                .isInstanceOf(RestApiException.class);
    }

    @Test
    @DisplayName("피해 팀에 대한 권한이 없는 모집자는 지원을 승인해 지원자를 그 팀에 가입시킬 수 없다")
    void updateDecisionStatus_byRecruiterWithoutTeamAuthority_isRejected() {
        // given: 공격자가 피해 팀의 모집자로 등록된 모집글과, 그에 대한 지원서가 존재
        actingAs(attacker);
        Recruitment recruitment = recruitmentRepository.save(Recruitment.builder()
                .title("무단 모집글")
                .category(Category.PROJECT)
                .description("피해 팀을 사칭한 모집글")
                .targetMemberCount(5)
                .endAt(LocalDateTime.now().plusDays(7))
                .team(team)
                .recruiter(attacker)
                .infoPost(null)
                .build());

        actingAs(applicant);
        RecruitmentApplication application = recruitmentApplicationRepository.save(RecruitmentApplication.builder()
                .recruitment(recruitment)
                .introduction("지원합니다")
                .build());

        ApplicationStatusUpdateRequest request = mock(ApplicationStatusUpdateRequest.class);
        when(request.getApplicationStatus()).thenReturn(Status.ACCEPTED);

        // when & then: 공격자는 모집글 작성자이지만 피해 팀에 대한 권한이 없으므로 승인이 거부되어야 한다
        actingAs(attacker);
        assertThatThrownBy(() -> recruitmentApplicationService.updateDecisionStatus(
                        application.getRecruitmentApplicationId(), request, attacker))
                .isInstanceOf(RestApiException.class);
    }

    private User createUser(String username, String email) {
        return userRepository.save(User.builder()
                .username(username)
                .email(email)
                .password("encoded-password")
                .name(username)
                .department(Department.COMPUTER_SCIENCE)
                .studentNumber(null)
                .isSchoolVerified(false)
                .role(Role.USER)
                .imageKey(null)
                .build());
    }

    private void actingAs(User user) {
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }
}
