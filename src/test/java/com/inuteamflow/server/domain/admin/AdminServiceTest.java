package com.inuteamflow.server.domain.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inuteamflow.server.domain.admin.dto.response.DashboardResponse;
import com.inuteamflow.server.domain.admin.enums.DashboardItemType;
import com.inuteamflow.server.domain.admin.service.AdminService;
import com.inuteamflow.server.domain.inquiry.dto.request.InquiryHandleRequest;
import com.inuteamflow.server.domain.inquiry.entity.Inquiry;
import com.inuteamflow.server.domain.inquiry.enums.InquiryType;
import com.inuteamflow.server.domain.inquiry.repository.InquiryRepository;
import com.inuteamflow.server.domain.notification.service.NotificationService;
import com.inuteamflow.server.domain.report.dto.request.ReportHandleRequest;
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
 * 관리자 대시보드 조회가 제대로 동작하는지 검증한다.
 *
 * <p>대시보드는 신고와 문의라는 서로 다른 두 테이블을 하나의 목록으로 병합해 페이지네이션하고,
 * 두 도메인의 처리 상태를 합산해 집계한다. 병합 정렬과 페이지 경계, 집계 합산을 중점적으로 확인한다.</p>
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

    @BeforeEach
    void setUp() {
        admin = saveUser("dashboard-admin", Role.ADMIN);
        reporter = saveUser("dashboard-reporter", Role.USER);
        target = saveUser("dashboard-target", Role.USER);
    }

    @Test
    @DisplayName("대시보드는 신고와 문의를 하나의 목록으로 합쳐 최신순으로 반환한다.")
    void getDashboard_mergesReportsAndInquiriesByLatest() {
        saveInterleavedItems();

        DashboardResponse response = adminService.getDashboard(PageRequest.of(0, 10, LATEST), admin);
        List<DashboardResponse.DashboardItem> items = response.getItems().getContent();

        assertThat(items).hasSize(4);
        assertThat(items)
                .extracting(DashboardResponse.DashboardItem::getDetail)
                .containsExactly("문의 2", "신고 2", "문의 1", "신고 1");
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
        saveInterleavedItems();

        DashboardResponse response = adminService.getDashboard(PageRequest.of(1, 2, LATEST), admin);

        assertThat(response.getItems().getContent())
                .extracting(DashboardResponse.DashboardItem::getDetail)
                .containsExactly("문의 1", "신고 1");
        assertThat(response.getItems().getTotalElements()).isEqualTo(4);
        assertThat(response.getItems().getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("대시보드 집계는 신고와 문의를 합산한다.")
    void getDashboard_summarySumsBothDomains() {
        saveUserReport("신고 1");
        saveUserReport("신고 2");
        saveInquiry("문의 1");

        DashboardResponse response = adminService.getDashboard(PageRequest.of(0, 10, LATEST), admin);

        assertThat(response.getSummary().getTotal()).isEqualTo(3);
        assertThat(response.getSummary().getPending()).isEqualTo(3);
        assertThat(response.getSummary().getResolved()).isZero();
    }

    @Test
    @DisplayName("신고를 처리하고 문의에 답변하면 대시보드 집계의 대기 건수와 완료 건수가 함께 반영된다.")
    void getDashboard_summaryReflectsHandledItems() throws JsonProcessingException {
        Report report = saveUserReport("처리될 신고");
        saveUserReport("남아 있는 신고");
        Inquiry inquiry = saveInquiry("답변될 문의");

        adminService.handleReport(report.getReportId(), objectMapper.readValue("""
                        { "userAction": { "action": "WARN", "detail": "경고" } }
                        """, ReportHandleRequest.class), admin);
        adminService.handleInquiry(
                inquiry.getInquiryId(), objectMapper.readValue("""
                        { "answer": "처리했습니다." }
                        """, InquiryHandleRequest.class), admin);

        DashboardResponse response = adminService.getDashboard(PageRequest.of(0, 10, LATEST), admin);

        assertThat(response.getSummary().getTotal()).isEqualTo(3);
        assertThat(response.getSummary().getPending()).isEqualTo(1);
        assertThat(response.getSummary().getResolved()).isEqualTo(2);
    }

    /**
     * 신고와 문의를 1분 간격으로 번갈아 저장한다.
     *
     * <p>{@code createdAt}이 같으면 병합 순서가 저장 순서에 의존하므로, 시각을 명시적으로 벌려
     * 시간순 정렬 자체를 검증할 수 있게 한다.</p>
     */
    private void saveInterleavedItems() {
        LocalDateTime base = LocalDateTime.of(2026, 8, 1, 10, 0);

        backdate("report", "report_id", saveUserReport("신고 1").getReportId(), base);
        backdate("inquiry", "inquiry_id", saveInquiry("문의 1").getInquiryId(), base.plusMinutes(1));
        backdate("report", "report_id", saveUserReport("신고 2").getReportId(), base.plusMinutes(2));
        backdate("inquiry", "inquiry_id", saveInquiry("문의 2").getInquiryId(), base.plusMinutes(3));

        entityManager.clear();
    }

    private void backdate(String table, String idColumn, Long id, LocalDateTime createdAt) {
        entityManager
                .createNativeQuery("UPDATE " + table + " SET created_at = :createdAt WHERE " + idColumn + " = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", id)
                .executeUpdate();
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
