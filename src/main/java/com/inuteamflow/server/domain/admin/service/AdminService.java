package com.inuteamflow.server.domain.admin.service;

import com.inuteamflow.server.domain.admin.dto.response.DashboardResponse;
import com.inuteamflow.server.domain.admin.dto.response.DashboardResponse.DashboardItem;
import com.inuteamflow.server.domain.infoPost.service.InfoPostService;
import com.inuteamflow.server.domain.inquiry.dto.request.InquiryHandleRequest;
import com.inuteamflow.server.domain.inquiry.dto.response.InquiryDetailResponse;
import com.inuteamflow.server.domain.inquiry.dto.response.InquirySummaryResponse;
import com.inuteamflow.server.domain.inquiry.dto.response.InquirySummaryResponse.InquirySummaryItem;
import com.inuteamflow.server.domain.inquiry.entity.Inquiry;
import com.inuteamflow.server.domain.inquiry.enums.InquiryStatus;
import com.inuteamflow.server.domain.inquiry.repository.InquiryRepository;
import com.inuteamflow.server.domain.recruitment.service.RecruitmentService;
import com.inuteamflow.server.domain.report.dto.request.ReportHandleRequest;
import com.inuteamflow.server.domain.report.dto.request.ReportHandleRequest.PostActionCommand;
import com.inuteamflow.server.domain.report.dto.request.ReportHandleRequest.UserActionCommand;
import com.inuteamflow.server.domain.report.dto.response.ReportDetailResponse;
import com.inuteamflow.server.domain.report.dto.response.ReportSummaryResponse;
import com.inuteamflow.server.domain.report.dto.response.ReportSummaryResponse.ReportSummaryItem;
import com.inuteamflow.server.domain.report.entity.Report;
import com.inuteamflow.server.domain.report.entity.ReportHandle;
import com.inuteamflow.server.domain.report.enums.PostActionType;
import com.inuteamflow.server.domain.report.enums.ReportStatus;
import com.inuteamflow.server.domain.report.enums.ReportTargetType;
import com.inuteamflow.server.domain.report.enums.UserActionType;
import com.inuteamflow.server.domain.report.repository.ReportHandleRepository;
import com.inuteamflow.server.domain.report.repository.ReportRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.global.dto.StatusSummary;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final InquiryRepository inquiryRepository;
    private final ReportRepository reportRepository;
    private final ReportHandleRepository reportHandleRepository;
    private final RecruitmentService recruitmentService;
    private final InfoPostService infoPostService;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 대시보드를 조회한다.
     *
     * <p>신고와 문의를 하나의 목록으로 통합해 최신순으로 반환하며, 두 항목을 합산한 처리 상태 집계를 함께 제공한다.</p>
     *
     * @param pageable 통합 목록의 페이지 정보
     * @param admin 조회를 요청한 관리자
     * @return 신고/문의 통합 집계와 통합 목록
     */
    public DashboardResponse getDashboard(Pageable pageable, User admin) {
        StatusSummary summary = StatusSummary.of(
                reportRepository.count() + inquiryRepository.count(),
                reportRepository.countByStatus(ReportStatus.PENDING)
                        + inquiryRepository.countByStatus(InquiryStatus.PENDING),
                reportRepository.countByStatus(ReportStatus.RESOLVED)
                        + inquiryRepository.countByStatus(InquiryStatus.RESOLVED));

        return DashboardResponse.of(summary, mergeLatestItems(pageable, summary.getTotal()));
    }

    /**
     * 신고 목록을 조회한다.
     *
     * @param pageable 신고 목록의 페이지 정보
     * @param admin 조회를 요청한 관리자
     * @param keyword 대상명/신고자명 검색 키워드
     * @return 신고 처리 상태 집계와 신고 목록
     */
    public ReportSummaryResponse getReports(Pageable pageable, User admin, String keyword) {
        StatusSummary summary = StatusSummary.of(
                reportRepository.count(),
                reportRepository.countByStatus(ReportStatus.PENDING),
                reportRepository.countByStatus(ReportStatus.RESOLVED));

        Page<ReportSummaryItem> reports = reportRepository
                .search(keyword, ReportTargetType.USER, pageable)
                .map(ReportSummaryItem::from);

        return ReportSummaryResponse.of(summary, reports);
    }

    /**
     * 신고 상세를 조회한다.
     *
     * <p>처리 완료된 신고는 게시글·사용자 조치 내역과 처리자 정보를 함께 반환한다.
     * 처리 대기 중이면 해당 항목은 모두 {@code null}이다.</p>
     *
     * @param reportId 조회할 신고 ID
     * @param admin 조회를 요청한 관리자
     * @return 신고 상세 정보
     * @throws RestApiException 신고를 찾을 수 없는 경우
     */
    public ReportDetailResponse getReport(Long reportId, User admin) {
        Report report = reportRepository
                .findById(reportId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.REPORT_NOT_FOUND));

        ReportHandle handle = reportHandleRepository.findByReport(report).orElse(null);

        return ReportDetailResponse.of(report, handle);
    }

    /**
     * 신고를 처리한다.
     *
     * <p>요청된 게시글·사용자 조치를 실제로 반영하고 처리 내역을 기록한 뒤, 신고 상태를 {@code RESOLVED}로 변경한다.
     * 처리자 정보는 처리 시점 기준으로 스냅샷 저장한다.</p>
     *
     * @param reportId 처리할 신고 ID
     * @param request 게시글·사용자 조치 내용
     * @param admin 처리를 수행한 관리자
     * @throws RestApiException 신고를 찾을 수 없거나 이미 처리된 경우
     */
    @Transactional
    public void handleReport(Long reportId, ReportHandleRequest request, User admin) {
        Report report = reportRepository
                .findById(reportId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.REPORT_NOT_FOUND));

        if (report.getStatus() == ReportStatus.RESOLVED) {
            throw new RestApiException(CustomErrorCode.REPORT_ALREADY_HANDLED);
        }

        PostActionCommand postAction = request.getPostAction();
        UserActionCommand userAction = request.getUserAction();
        validateHandleRequest(report, postAction, userAction);

        applyPostAction(report, postAction);
        applyUserAction(report, userAction);

        reportHandleRepository.save(ReportHandle.create(
                report,
                admin.getUserId(),
                admin.getName(),
                postAction == null ? null : postAction.getAction(),
                postAction == null ? null : postAction.getDetail(),
                userAction.getAction(),
                userAction.getDurationDays(),
                userAction.getDetail()));

        report.resolve();
    }

    /**
     * 문의 목록을 조회한다.
     *
     * @param pageable 문의 목록의 페이지 정보
     * @param admin 조회를 요청한 관리자
     * @param keyword 문의 내용/문의자명 검색 키워드
     * @return 문의 처리 상태 집계와 문의 목록
     */
    public InquirySummaryResponse getInquiries(Pageable pageable, User admin, String keyword) {
        StatusSummary summary = StatusSummary.of(
                inquiryRepository.count(),
                inquiryRepository.countByStatus(InquiryStatus.PENDING),
                inquiryRepository.countByStatus(InquiryStatus.RESOLVED));

        Page<InquirySummaryItem> inquiries =
                inquiryRepository.search(keyword, pageable).map(InquirySummaryItem::from);

        return InquirySummaryResponse.of(summary, inquiries);
    }

    /**
     * 문의 상세를 조회한다.
     *
     * <p>관리자는 문의자 본인 여부와 관계없이 모든 문의를 조회할 수 있다.</p>
     *
     * @param inquiryId 조회할 문의 ID
     * @param admin 조회를 요청한 관리자
     * @return 문의 상세 정보
     * @throws RestApiException 문의를 찾을 수 없는 경우
     */
    public InquiryDetailResponse getInquiry(Long inquiryId, User admin) {
        Inquiry inquiry = inquiryRepository
                .findById(inquiryId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.INQUIRY_NOT_FOUND));

        return InquiryDetailResponse.from(inquiry);
    }

    /**
     * 문의에 답변한다.
     *
     * <p>답변자 정보를 답변 시점 기준으로 스냅샷 저장하고, 문의 상태를 {@code RESOLVED}로 변경한다.</p>
     *
     * @param inquiryId 답변할 문의 ID
     * @param request 답변 내용
     * @param admin 답변을 작성한 관리자
     * @throws RestApiException 문의를 찾을 수 없거나 이미 답변된 경우
     */
    @Transactional
    public void handleInquiry(Long inquiryId, InquiryHandleRequest request, User admin) {
        Inquiry inquiry = inquiryRepository
                .findById(inquiryId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.INQUIRY_NOT_FOUND));

        if (inquiry.getStatus() == InquiryStatus.RESOLVED) {
            throw new RestApiException(CustomErrorCode.INQUIRY_ALREADY_ANSWERED);
        }

        inquiry.answer(
                request.getAnswer(), admin.getUserId(), admin.getName(), LocalDateTime.now(ZoneId.systemDefault()));
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 신고와 문의를 최신순으로 병합한 대시보드 목록 페이지를 만든다.
     *
     * <p>병합 목록의 상위 N건은 각 목록의 상위 N건 안에만 존재하므로, 두 테이블에서 각각
     * {@code offset + size}건만 조회해도 정확한 페이지를 구성할 수 있다.
     * 두 테이블의 합집합을 정렬해야 하므로 요청된 정렬 조건은 무시하고 {@code createdAt} 내림차순으로 고정한다.</p>
     *
     * @param pageable 병합 목록의 페이지 정보
     * @param total 신고와 문의를 합한 전체 건수
     * @return 최신순으로 병합된 대시보드 항목 페이지
     */
    private Page<DashboardItem> mergeLatestItems(Pageable pageable, long total) {
        Pageable latest = PageRequest.of(
                0, (int) pageable.getOffset() + pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));

        List<DashboardItem> content = Stream.concat(
                        reportRepository.findAll(latest).stream().map(DashboardItem::fromReport),
                        inquiryRepository.findAll(latest).stream().map(DashboardItem::fromInquiry))
                .sorted(Comparator.comparing(DashboardItem::getCreatedAt).reversed())
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 신고 처리 요청이 신고 대상과 정합한지 검증한다.
     *
     * <p>사용자 신고에는 게시글 조치를 지정할 수 없고, 게시글 조치로 {@code DELETE}를 선택하면 대상 게시글이 남아 있어야 한다.
     * 사용자 조치로 {@code SUSPEND}를 선택한 경우 정지 일수는 1 이상이어야 한다.</p>
     *
     * @param report 처리 대상 신고
     * @param postAction 게시글 조치 요청, 없으면 {@code null}
     * @param userAction 사용자 조치 요청
     * @throws RestApiException 요청이 신고 대상과 맞지 않는 경우
     */
    private void validateHandleRequest(Report report, PostActionCommand postAction, UserActionCommand userAction) {
        boolean isUserReport = report.getTargetType() == ReportTargetType.USER;

        if (isUserReport && postAction != null) {
            throw new RestApiException(CustomErrorCode.REPORT_HANDLE_INVALID);
        }
        if (!isUserReport && postAction == null) {
            throw new RestApiException(CustomErrorCode.REPORT_HANDLE_INVALID);
        }
        if (userAction.getAction() == UserActionType.SUSPEND
                && (userAction.getDurationDays() == null || userAction.getDurationDays() < 1)) {
            throw new RestApiException(CustomErrorCode.REPORT_HANDLE_INVALID);
        }
    }

    /**
     * 신고 대상 게시글에 조치를 반영한다.
     *
     * <p>{@code DELETE}인 경우에만 대상 유형에 맞는 게시글을 강제 삭제한다.</p>
     *
     * @param report 처리 대상 신고
     * @param postAction 게시글 조치 요청, 없으면 {@code null}
     */
    private void applyPostAction(Report report, PostActionCommand postAction) {
        if (postAction == null || postAction.getAction() != PostActionType.DELETE) {
            return;
        }

        if (report.getTargetType() == ReportTargetType.RECRUITMENT_POST) {
            recruitmentService.deleteRecruitmentByAdmin(report.getTargetPostId());
        } else {
            infoPostService.deleteInfoPostByAdmin(report.getTargetPostId());
        }
    }

    /**
     * 신고 대상 사용자에게 조치를 반영한다.
     *
     * <p>{@code SUSPEND}는 요청된 일수만큼 정지 만료 시각을 설정하고, {@code BAN}은 권한을 영구 정지로 변경한다.
     * {@code WARN}과 {@code NONE}은 기록만 남기므로 사용자 상태를 바꾸지 않는다.
     * 대상 사용자가 이미 탈퇴한 경우 조치를 건너뛴다.</p>
     *
     * @param report 처리 대상 신고
     * @param userAction 사용자 조치 요청
     */
    private void applyUserAction(Report report, UserActionCommand userAction) {
        if (userAction.getAction() != UserActionType.SUSPEND && userAction.getAction() != UserActionType.BAN) {
            return;
        }
        if (report.getTargetUserId() == null) {
            return;
        }

        userRepository.findById(report.getTargetUserId()).ifPresent(target -> {
            if (userAction.getAction() == UserActionType.SUSPEND) {
                target.suspend(LocalDateTime.now(ZoneId.systemDefault()).plusDays(userAction.getDurationDays()));
            } else {
                target.ban();
            }
        });
    }
}
