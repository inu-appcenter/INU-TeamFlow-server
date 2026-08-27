package com.inuteamflow.server.domain.report.dto.response;

import com.inuteamflow.server.domain.report.entity.Report;
import com.inuteamflow.server.domain.report.enums.ReportReason;
import com.inuteamflow.server.domain.report.enums.ReportStatus;
import com.inuteamflow.server.domain.report.enums.ReportTargetType;
import com.inuteamflow.server.global.dto.StatusSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "신고 목록 응답 DTO")
public class ReportSummaryResponse {

    @Schema(description = "신고 집계")
    private StatusSummary summary;

    @Schema(description = "신고 목록 (페이지네이션)")
    private Page<ReportSummaryItem> reports;

    public static ReportSummaryResponse of(StatusSummary summary, Page<ReportSummaryItem> reports) {
        return new ReportSummaryResponse(summary, reports);
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Schema(description = "신고 목록 항목 DTO")
    public static class ReportSummaryItem {

        @Schema(description = "신고 ID", example = "12")
        private Long reportId;

        @Schema(description = "신고 사유", example = "ABUSE")
        private ReportReason reason;

        @Schema(description = "상세 내용")
        private String detail;

        @Schema(description = "신고 대상 유형", example = "USER")
        private ReportTargetType targetType;

        @Schema(description = "대상 이름 (USER면 유저명, 게시글이면 글 제목)", example = "이OO")
        private String targetName;

        @Schema(description = "신고자 이름", example = "김OO")
        private String reporterName;

        @Schema(description = "처리 상태", example = "PENDING")
        private ReportStatus status;

        @Schema(description = "신고 접수 시각")
        private LocalDateTime createdAt;

        public static ReportSummaryItem from(Report report) {
            String targetName = report.getTargetType() == ReportTargetType.USER
                    ? report.getTargetUserName()
                    : report.getTargetPostTitle();

            return new ReportSummaryItem(
                    report.getReportId(),
                    report.getReason(),
                    report.getDetail(),
                    report.getTargetType(),
                    targetName,
                    report.getReporterName(),
                    report.getStatus(),
                    report.getCreatedAt());
        }
    }
}
