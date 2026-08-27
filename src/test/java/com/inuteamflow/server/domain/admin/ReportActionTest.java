package com.inuteamflow.server.domain.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inuteamflow.server.domain.admin.service.AdminService;
import com.inuteamflow.server.domain.infoPost.entity.InfoPost;
import com.inuteamflow.server.domain.infoPost.entity.InfoPostImage;
import com.inuteamflow.server.domain.infoPost.enums.InfoPostCategory;
import com.inuteamflow.server.domain.infoPost.repository.InfoPostImageRepository;
import com.inuteamflow.server.domain.infoPost.repository.InfoPostRepository;
import com.inuteamflow.server.domain.notification.service.NotificationService;
import com.inuteamflow.server.domain.recruitment.entity.Recruitment;
import com.inuteamflow.server.domain.recruitment.repository.RecruitmentRepository;
import com.inuteamflow.server.domain.report.dto.request.ReportHandleRequest;
import com.inuteamflow.server.domain.report.dto.response.ReportDetailResponse;
import com.inuteamflow.server.domain.report.entity.Report;
import com.inuteamflow.server.domain.report.enums.ReportReason;
import com.inuteamflow.server.domain.report.enums.ReportStatus;
import com.inuteamflow.server.domain.report.enums.ReportTargetType;
import com.inuteamflow.server.domain.report.enums.UserActionType;
import com.inuteamflow.server.domain.report.repository.ReportRepository;
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
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
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
 * 신고 처리 흐름이 제대로 동작하는지 검증한다.
 *
 * <p>신고 처리는 요청된 조치를 실제로 반영한 뒤 처리 내역을 기록하고 신고 상태를 완료로 바꾸는 과정이다.
 * 게시글 강제 삭제는 연관 데이터(이미지·S3 객체)까지 정리되어야 하고, 사용자 조치는 실제 정지 상태로 이어져야 하며,
 * 조치가 없는 경우에는 어떤 상태도 바뀌지 않아야 한다. 신고 대상과 맞지 않는 요청은 조치 반영 전에 거부되어야 한다.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReportActionTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private InfoPostRepository infoPostRepository;

    @Autowired
    private InfoPostImageRepository infoPostImageRepository;

    @Autowired
    private RecruitmentRepository recruitmentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private S3Service s3Service;

    @MockitoBean
    private NotificationService notificationService;

    private User admin;
    private User reporter;
    private User target;

    @BeforeEach
    void setUp() {
        admin = saveUser("action-admin", Role.ADMIN);
        reporter = saveUser("action-reporter", Role.USER);
        target = saveUser("action-target", Role.USER);
        actingAs(target);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("정보글 신고를 삭제로 처리하면 정보글과 이미지가 함께 삭제된다.")
    void deleteAction_removesInfoPostWithImages() throws JsonProcessingException {
        InfoPost infoPost =
                infoPostRepository.saveAndFlush(InfoPost.create(InfoPostCategory.CONTEST, "신고된 정보글", "부적절한 내용"));
        infoPostImageRepository.saveAndFlush(InfoPostImage.create("images/reported-1.jpg", 0, infoPost));
        Report report = savePostReport(ReportTargetType.INFO_POST, infoPost.getInfoPostId(), infoPost.getTitle());

        adminService.handleReport(report.getReportId(), deleteRequest(), admin);

        assertThat(infoPostRepository.findById(infoPost.getInfoPostId())).isEmpty();
        assertThat(infoPostImageRepository.findByInfoPostOrderBySortOrderAsc(infoPost))
                .isEmpty();
        verify(s3Service, times(1)).deleteImage("images/reported-1.jpg");
        assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
    }

    @Test
    @DisplayName("모집글 신고를 삭제로 처리하면 모집글이 삭제된다.")
    void deleteAction_removesRecruitment() throws JsonProcessingException {
        Recruitment recruitment = saveRecruitment();
        Report report = savePostReport(
                ReportTargetType.RECRUITMENT_POST, recruitment.getRecruitmentId(), recruitment.getTitle());

        adminService.handleReport(report.getReportId(), deleteRequest(), admin);

        assertThat(recruitmentRepository.findById(recruitment.getRecruitmentId()))
                .isEmpty();
        assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
    }

    @Test
    @DisplayName("게시글 조치가 유지이면 게시글이 삭제되지 않는다.")
    void keepAction_doesNotRemoveInfoPost() throws JsonProcessingException {
        InfoPost infoPost =
                infoPostRepository.saveAndFlush(InfoPost.create(InfoPostCategory.CONTEST, "유지되는 정보글", "문제 없는 내용"));
        Report report = savePostReport(ReportTargetType.INFO_POST, infoPost.getInfoPostId(), infoPost.getTitle());

        adminService.handleReport(report.getReportId(), handleRequest("""
                        {
                          "postAction": { "action": "NONE", "detail": "문제 없음" },
                          "userAction": { "action": "NONE", "detail": "조치 없음" }
                        }
                        """), admin);

        assertThat(infoPostRepository.findById(infoPost.getInfoPostId())).isPresent();
        verify(s3Service, never()).deleteImage(anyString());
    }

    @Test
    @DisplayName("정지 조치는 요청한 일수만큼 대상 사용자의 정지 만료 시각을 설정한다.")
    void suspendAction_setsSuspendedUntilByDuration() throws JsonProcessingException {
        Report report = saveUserReport();
        LocalDateTime before = LocalDateTime.now();

        adminService.handleReport(report.getReportId(), handleRequest("""
                        { "userAction": { "action": "SUSPEND", "durationDays": 7, "detail": "7일 정지" } }
                        """), admin);

        assertThat(target.isSuspended()).isTrue();
        assertThat(target.getSuspendedUntil()).isAfter(before.plusDays(6)).isBefore(before.plusDays(8));
        assertThat(target.getRole()).isEqualTo(Role.USER);

        ReportDetailResponse response = adminService.getReport(report.getReportId(), admin);
        assertThat(response.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(response.getUserAction().getAction()).isEqualTo(UserActionType.SUSPEND);
        assertThat(response.getUserAction().getDurationDays()).isEqualTo(7);
        assertThat(response.getPostAction()).isNull();
        assertThat(response.getHandledBy().getUserId()).isEqualTo(admin.getUserId());
        assertThat(response.getHandledAt()).isNotNull();
    }

    @Test
    @DisplayName("처리 대기 중인 신고의 상세에는 조치 내역이 담기지 않는다.")
    void pendingReport_hasNoHandleInfo() {
        Report report = saveUserReport();

        ReportDetailResponse response = adminService.getReport(report.getReportId(), admin);

        assertThat(response.getStatus()).isEqualTo(ReportStatus.PENDING);
        assertThat(response.getPostAction()).isNull();
        assertThat(response.getUserAction()).isNull();
        assertThat(response.getHandledBy()).isNull();
        assertThat(response.getHandledAt()).isNull();
    }

    @Test
    @DisplayName("이미 처리된 신고는 다시 처리할 수 없다.")
    void alreadyHandledReport_throws() throws JsonProcessingException {
        Report report = saveUserReport();
        adminService.handleReport(report.getReportId(), warnRequest(), admin);

        assertThatThrownBy(() -> adminService.handleReport(report.getReportId(), warnRequest(), admin))
                .isInstanceOf(RestApiException.class)
                .extracting(exception -> ((RestApiException) exception).getErrorCode())
                .isEqualTo(CustomErrorCode.REPORT_ALREADY_HANDLED);
    }

    @Test
    @DisplayName("사용자 신고에 게시글 조치를 지정하면 처리되지 않는다.")
    void postActionOnUserReport_throws() throws JsonProcessingException {
        Report report = saveUserReport();

        assertThatThrownBy(() -> adminService.handleReport(report.getReportId(), deleteRequest(), admin))
                .isInstanceOf(RestApiException.class)
                .extracting(exception -> ((RestApiException) exception).getErrorCode())
                .isEqualTo(CustomErrorCode.REPORT_HANDLE_INVALID);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
    }

    @Test
    @DisplayName("정지 조치에 정지 일수가 없으면 처리되지 않고 대상 사용자도 정지되지 않는다.")
    void suspendWithoutDuration_throws() throws JsonProcessingException {
        Report report = saveUserReport();

        ReportHandleRequest request = handleRequest("""
                { "userAction": { "action": "SUSPEND", "detail": "일수 없음" } }
                """);

        assertThatThrownBy(() -> adminService.handleReport(report.getReportId(), request, admin))
                .isInstanceOf(RestApiException.class)
                .extracting(exception -> ((RestApiException) exception).getErrorCode())
                .isEqualTo(CustomErrorCode.REPORT_HANDLE_INVALID);
        assertThat(target.isSuspended()).isFalse();
        assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
    }

    @Test
    @DisplayName("영구 정지 조치는 대상 사용자의 권한만 BANNED로 바꾸고 정지 시각은 남기지 않는다.")
    void banAction_changesRoleOnly() throws JsonProcessingException {
        Report report = saveUserReport();

        adminService.handleReport(report.getReportId(), handleRequest("""
                        { "userAction": { "action": "BAN", "detail": "영구 정지" } }
                        """), admin);

        assertThat(target.getRole()).isEqualTo(Role.BANNED);
        assertThat(target.getSuspendedUntil()).isNull();
    }

    @Test
    @DisplayName("경고 조치는 대상 사용자의 상태를 바꾸지 않는다.")
    void warnAction_doesNotChangeUserState() throws JsonProcessingException {
        Report report = saveUserReport();

        adminService.handleReport(report.getReportId(), handleRequest("""
                        { "userAction": { "action": "WARN", "detail": "경고" } }
                        """), admin);

        assertThat(target.isSuspended()).isFalse();
        assertThat(target.getRole()).isEqualTo(Role.USER);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
    }

    @Test
    @DisplayName("사용자 조치는 신고자가 아닌 신고 대상에게 적용된다.")
    void userAction_appliesToTargetNotReporter() throws JsonProcessingException {
        Report report = saveUserReport();

        adminService.handleReport(report.getReportId(), handleRequest("""
                        { "userAction": { "action": "SUSPEND", "durationDays": 3, "detail": "3일 정지" } }
                        """), admin);

        assertThat(target.isSuspended()).isTrue();
        assertThat(reporter.isSuspended()).isFalse();
        assertThat(admin.isSuspended()).isFalse();
    }

    private Recruitment saveRecruitment() {
        Team team = teamRepository.save(Team.builder()
                .name("신고 대상 팀")
                .description("테스트 팀 설명")
                .category(Category.PROJECT)
                .build());
        teamMemberRepository.save(TeamMember.create(team, target, TeamRole.LEADER));

        return recruitmentRepository.saveAndFlush(Recruitment.builder()
                .title("신고된 모집글")
                .description("부적절한 모집글")
                .category(Category.PROJECT)
                .targetMemberCount(4)
                .endAt(LocalDateTime.now().plusDays(7))
                .team(team)
                .recruiter(target)
                .build());
    }

    private Report savePostReport(ReportTargetType targetType, Long postId, String postTitle) {
        return reportRepository.saveAndFlush(Report.create(
                reporter.getUserId(),
                reporter.getName(),
                targetType,
                postId,
                postTitle,
                target.getUserId(),
                target.getName(),
                ReportReason.INAPPROPRIATE,
                "부적절한 게시글 신고"));
    }

    private Report saveUserReport() {
        return reportRepository.saveAndFlush(Report.create(
                reporter.getUserId(),
                reporter.getName(),
                ReportTargetType.USER,
                null,
                null,
                target.getUserId(),
                target.getName(),
                ReportReason.ABUSE,
                "욕설 신고"));
    }

    private ReportHandleRequest deleteRequest() throws JsonProcessingException {
        return handleRequest("""
                {
                  "postAction": { "action": "DELETE", "detail": "강제 삭제" },
                  "userAction": { "action": "NONE", "detail": "조치 없음" }
                }
                """);
    }

    private ReportHandleRequest warnRequest() throws JsonProcessingException {
        return handleRequest("""
                { "userAction": { "action": "WARN", "detail": "경고" } }
                """);
    }

    private ReportHandleRequest handleRequest(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, ReportHandleRequest.class);
    }

    private User saveUser(String username, Role role) {
        return userRepository.save(User.builder()
                .username(username)
                .email(username + "@inu.ac.kr")
                .password("encoded-password")
                .name(username)
                .department(Department.COMPUTER_SCIENCE)
                .isSchoolVerified(false)
                .role(role)
                .build());
    }

    private void actingAs(User user) {
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }
}
