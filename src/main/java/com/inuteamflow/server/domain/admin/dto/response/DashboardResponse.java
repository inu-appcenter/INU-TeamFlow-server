package com.inuteamflow.server.domain.admin.dto.response;

import com.inuteamflow.server.domain.admin.enums.DashboardItemType;
import com.inuteamflow.server.domain.inquiry.entity.Inquiry;
import com.inuteamflow.server.domain.report.entity.Report;
import com.inuteamflow.server.global.dto.StatusSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "관리자 대시보드 응답 DTO")
public class DashboardResponse {

    @Schema(description = "신고/문의 통합 집계")
    private StatusSummary summary;

    @Schema(description = "신고/문의 통합 목록 (페이지네이션)")
    private Page<DashboardItem> items;

    public static DashboardResponse of(StatusSummary summary, Page<DashboardItem> items) {
        return new DashboardResponse(summary, items);
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Schema(description = "대시보드 항목 (신고/문의 통합)")
    public static class DashboardItem {

        @Schema(description = "항목 유형", example = "REPORT")
        private DashboardItemType itemType;

        @Schema(description = "원본 ID (reportId 또는 inquiryId)", example = "12")
        private Long refId;

        @Schema(description = "상세 내용")
        private String detail;

        @Schema(description = "처리 상태", example = "PENDING")
        private String status;

        @Schema(description = "접수 시각")
        private LocalDateTime createdAt;

        public static DashboardItem fromReport(Report report) {
            return new DashboardItem(
                    DashboardItemType.REPORT,
                    report.getReportId(),
                    report.getDetail(),
                    report.getStatus().name(),
                    report.getCreatedAt());
        }

        public static DashboardItem fromInquiry(Inquiry inquiry) {
            return new DashboardItem(
                    DashboardItemType.INQUIRY,
                    inquiry.getInquiryId(),
                    inquiry.getDetail(),
                    inquiry.getStatus().name(),
                    inquiry.getCreatedAt());
        }
    }
}
