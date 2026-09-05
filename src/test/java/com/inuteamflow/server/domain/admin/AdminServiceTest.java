package com.inuteamflow.server.domain.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inuteamflow.server.domain.admin.dto.response.DashboardResponse;
import com.inuteamflow.server.domain.admin.enums.DashboardItemType;
import com.inuteamflow.server.domain.admin.service.AdminService;
import com.inuteamflow.server.domain.inquiry.dto.request.InquiryHandleRequest;
import com.inuteamflow.server.domain.inquiry.dto.response.InquirySummaryResponse;
import com.inuteamflow.server.domain.inquiry.dto.response.InquirySummaryResponse.InquirySummaryItem;
import com.inuteamflow.server.domain.inquiry.entity.Inquiry;
import com.inuteamflow.server.domain.inquiry.enums.InquiryType;
import com.inuteamflow.server.domain.inquiry.repository.InquiryRepository;
import com.inuteamflow.server.domain.notification.service.NotificationService;
import com.inuteamflow.server.domain.report.dto.request.ReportHandleRequest;
import com.inuteamflow.server.domain.report.dto.response.ReportSummaryResponse;
import com.inuteamflow.server.domain.report.dto.response.ReportSummaryResponse.ReportSummaryItem;
import com.inuteamflow.server.domain.report.entity.Report;
import com.inuteamflow.server.domain.report.enums.ReportReason;
import com.inuteamflow.server.domain.report.enums.ReportTargetType;
import com.inuteamflow.server.domain.report.repository.ReportRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.enums.Department;
import com.inuteamflow.server.domain.user.enums.Role;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.global.s3.S3Service;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 대시보드와 신고/문의 목록 조회가 제대로 동작하는지 검증한다.
 *
 * <p>대시보드는 신고와 문의라는 서로 다른 두 테이블을 하나의 목록으로 병합해 페이지네이션하고,
 * 두 도메인의 처리 상태를 합산해 집계한다. 병합 정렬과 페이지 경계, 집계 합산을 중점적으로 확인한다.</p>
 *
 * <p>목록 조회는 키워드 검색이 현재 페이지가 아니라 전체를 대상으로 하는지, 그리고 검색 중에도
 * 처리 상태 집계가 전체 기준을 유지하는지를 확인한다.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminServiceTest {

    private static final Sort LATEST = Sort.by(Sort.Direction.DESC, "createdAt");

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @MockitoBean
    private S3Service s3Service;

    @MockitoBean
    private NotificationService notificationService;

    private User admin;
    private User reporter;
    private User target;

    private Report userReport;
    private Inquiry loginInquiry;

    /**
     * 신고 2건과 문의 2건을 1분 간격으로 번갈아 저장한다.
     *
     * <p>{@code createdAt}이 같으면 병합 순서가 저장 순서에 의존하므로, 시각을 명시적으로 벌려
     * 시간순 정렬 자체를 검증할 수 있게 한다.</p>
     *
     * <p>신고 2건은 대상 유형을 게시글과 사용자로 나누되 대상 사용자는 동일하게 둔다.
     * 목록에 표시되는 대상명이 유형에 따라 갈리는 것을 검색 테스트에서 확인하기 위해서다.</p>
     */
    @BeforeEach
    void setUp() {
        admin = saveUser("dashboard-admin", Role.ADMIN);
        reporter = saveUser("dashboard-reporter", Role.USER);
        target = saveUser("dashboard-target", Role.USER);

        LocalDateTime base = LocalDateTime.of(2026, 8, 1, 10, 0);

        Report postReport = savePostReport("스터디 모집 글", "신고 1");
        loginInquiry = saveInquiry("로그인이 안 됩니다");
        userReport = saveUserReport("신고 2");
        Inquiry alarmInquiry = saveInquiry("알림이 오지 않습니다");

        backdate("report", "report_id", postReport.getReportId(), base);
        backdate("inquiry", "inquiry_id", loginInquiry.getInquiryId(), base.plusMinutes(1));
        backdate("report", "report_id", userReport.getReportId(), base.plusMinutes(2));
        backdate("inquiry", "inquiry_id", alarmInquiry.getInquiryId(), base.plusMinutes(3));

        entityManager.clear();
    }

    @Test
    @DisplayName("대시보드는 신고와 문의를 하나의 목록으로 합쳐 최신순으로 반환한다.")
    void getDashboard_mergesReportsAndInquiriesByLatest() {
        DashboardResponse response = adminService.getDashboard(PageRequest.of(0, 10, LATEST), admin);
        List<DashboardResponse.DashboardItem> items = response.getItems().getContent();

        assertThat(items).hasSize(4);
        assertThat(items)
                .extracting(DashboardResponse.DashboardItem::getDetail)
                .containsExactly("알림이 오지 않습니다", "신고 2", "로그인이 안 됩니다", "신고 1");
        assertThat(items)
                .extracting(DashboardResponse.DashboardItem::getItemType)
                .containsExactly(
                        DashboardItemType.INQUIRY,
                        DashboardItemType.REPORT,
                        DashboardItemType.INQUIRY,
                        DashboardItemType.REPORT);
    }

    @Test
    @DisplayName("대시보드 두 번째 페이지는 병합 목록에서 건너뛴 이후 항목을 반환한다.")
    void getDashboard_returnsSecondPageOfMergedList() {
        DashboardResponse response = adminService.getDashboard(PageRequest.of(1, 2, LATEST), admin);

        assertThat(response.getItems().getContent())
                .extracting(DashboardResponse.DashboardItem::getDetail)
                .containsExactly("로그인이 안 됩니다", "신고 1");
        assertThat(response.getItems().getTotalElements()).isEqualTo(4);
        assertThat(response.getItems().getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("대시보드 집계는 신고와 문의를 합산한다.")
    void getDashboard_summarySumsBothDomains() {
        DashboardResponse response = adminService.getDashboard(PageRequest.of(0, 10, LATEST), admin);

        assertThat(response.getSummary().getTotal()).isEqualTo(4);
        assertThat(response.getSummary().getPending()).isEqualTo(4);
        assertThat(response.getSummary().getResolved()).isZero();
    }

    @Test
    @DisplayName("신고를 처리하고 문의에 답변하면 대시보드 집계의 대기 건수와 완료 건수가 함께 반영된다.")
    void getDashboard_summaryReflectsHandledItems() throws JsonProcessingException {
        adminService.handleReport(
                userReport.getReportId(), objectMapper.readValue("""
                        { "userAction": { "action": "WARN", "detail": "경고" } }
                        """, ReportHandleRequest.class), admin);
        adminService.handleInquiry(
                loginInquiry.getInquiryId(), objectMapper.readValue("""
                        { "answer": "처리했습니다." }
                        """, InquiryHandleRequest.class), admin);

        DashboardResponse response = adminService.getDashboard(PageRequest.of(0, 10, LATEST), admin);

        assertThat(response.getSummary().getTotal()).isEqualTo(4);
        assertThat(response.getSummary().getPending()).isEqualTo(2);
        assertThat(response.getSummary().getResolved()).isEqualTo(2);
    }

    @Test
    @DisplayName("신고 목록을 대상명과 신고자명으로 검색하며, 검색 범위는 현재 페이지가 아니라 전체다.")
    void getReports_withKeyword_matchesTargetNameAndReporterName() {
        ReportSummaryResponse byTargetName = adminService.getReports(PageRequest.of(0, 1, LATEST), admin, "스터디");
        assertThat(byTargetName.getReports().getTotalElements()).isEqualTo(1);
        assertThat(byTargetName.getReports().getContent())
                .extracting(ReportSummaryItem::getTargetName)
                .containsExactly("스터디 모집 글");

        ReportSummaryResponse byReporterName =
                adminService.getReports(PageRequest.of(0, 10, LATEST), admin, reporter.getName());
        assertThat(byReporterName.getReports().getTotalElements()).isEqualTo(2);

        ReportSummaryResponse noMatch = adminService.getReports(PageRequest.of(0, 10, LATEST), admin, "없는키워드");
        assertThat(noMatch.getReports().getTotalElements()).isZero();
    }

    @Test
    @DisplayName("게시글 신고는 목록에 표시되지 않는 작성자명으로는 검색되지 않는다.")
    void getReports_withKeyword_doesNotMatchHiddenPostAuthorName() {
        ReportSummaryResponse response =
                adminService.getReports(PageRequest.of(0, 10, LATEST), admin, target.getName());

        assertThat(response.getReports().getTotalElements()).isEqualTo(1);
        assertThat(response.getReports().getContent())
                .extracting(ReportSummaryItem::getTargetName)
                .containsExactly(target.getName());
    }

    @Test
    @DisplayName("키워드가 없으면 신고 전체를 조회한다.")
    void getReports_withoutKeyword_returnsAllReports() {
        ReportSummaryResponse response = adminService.getReports(PageRequest.of(0, 10, LATEST), admin, null);

        assertThat(response.getReports().getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("검색 중에도 신고 집계는 전체 기준을 유지한다.")
    void getReports_withKeyword_keepsGlobalSummary() {
        ReportSummaryResponse response = adminService.getReports(PageRequest.of(0, 10, LATEST), admin, "스터디");

        assertThat(response.getReports().getTotalElements()).isEqualTo(1);
        assertThat(response.getSummary().getTotal()).isEqualTo(2);
        assertThat(response.getSummary().getPending()).isEqualTo(2);
    }

    @Test
    @DisplayName("문의 목록을 내용과 문의자명으로 검색하며, 검색 범위는 현재 페이지가 아니라 전체다.")
    void getInquiries_withKeyword_matchesDetailAndInquirerName() {
        InquirySummaryResponse byDetail = adminService.getInquiries(PageRequest.of(0, 1, LATEST), admin, "로그인");
        assertThat(byDetail.getInquiries().getTotalElements()).isEqualTo(1);
        assertThat(byDetail.getInquiries().getContent())
                .extracting(InquirySummaryItem::getDetail)
                .containsExactly("로그인이 안 됩니다");

        InquirySummaryResponse byInquirerName =
                adminService.getInquiries(PageRequest.of(0, 10, LATEST), admin, reporter.getName());
        assertThat(byInquirerName.getInquiries().getTotalElements()).isEqualTo(2);

        InquirySummaryResponse noMatch = adminService.getInquiries(PageRequest.of(0, 10, LATEST), admin, "없는키워드");
        assertThat(noMatch.getInquiries().getTotalElements()).isZero();
    }

    @Test
    @DisplayName("키워드가 없으면 문의 전체를 조회한다.")
    void getInquiries_withoutKeyword_returnsAllInquiries() {
        InquirySummaryResponse response = adminService.getInquiries(PageRequest.of(0, 10, LATEST), admin, null);

        assertThat(response.getInquiries().getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("검색 중에도 문의 집계는 전체 기준을 유지한다.")
    void getInquiries_withKeyword_keepsGlobalSummary() {
        InquirySummaryResponse response = adminService.getInquiries(PageRequest.of(0, 10, LATEST), admin, "로그인");

        assertThat(response.getInquiries().getTotalElements()).isEqualTo(1);
        assertThat(response.getSummary().getTotal()).isEqualTo(2);
        assertThat(response.getSummary().getPending()).isEqualTo(2);
    }

    private void backdate(String table, String idColumn, Long id, LocalDateTime createdAt) {
        entityManager
                .createNativeQuery("UPDATE " + table + " SET created_at = :createdAt WHERE " + idColumn + " = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", id)
                .executeUpdate();
    }

    /**
     * 게시글 신고를 저장한다.
     *
     * <p>게시글 신고는 글 제목과 함께 작성자 이름도 스냅샷 저장하지만, 목록에 표시되는 대상명은 글 제목이다.
     * 검색이 이 구분을 지키는지 확인하기 위해 두 값을 모두 채운다.</p>
     */
    private Report savePostReport(String postTitle, String detail) {
        return reportRepository.saveAndFlush(Report.create(
                reporter.getUserId(),
                reporter.getName(),
                ReportTargetType.INFO_POST,
                1L,
                postTitle,
                target.getUserId(),
                target.getName(),
                ReportReason.ABUSE,
                detail));
    }

    private Report saveUserReport(String detail) {
        return reportRepository.saveAndFlush(Report.create(
                reporter.getUserId(),
                reporter.getName(),
                ReportTargetType.USER,
                null,
                null,
                target.getUserId(),
                target.getName(),
                ReportReason.ABUSE,
                detail));
    }

    private Inquiry saveInquiry(String detail) {
        return inquiryRepository.saveAndFlush(
                Inquiry.create(reporter.getUserId(), reporter.getName(), InquiryType.BUG, detail));
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
}
