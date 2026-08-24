package com.inuteamflow.server.domain.report.dto.response;

import com.inuteamflow.server.domain.report.entity.Report;
import com.inuteamflow.server.domain.report.enums.ReportReason;
import com.inuteamflow.server.domain.report.enums.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "신고 응답 DTO")
public class ReportResponse {

    @Schema(description = "신고 ID", example = "1")
    private Long reportId;

    @Schema(description = "신고 사유", example = "SPAM")
    private ReportReason reason;

    @Schema(description = "상세 내용")
    private String detail;

    @Schema(description = "처리 상태", example = "PENDING")
    private ReportStatus status;

    @Schema(description = "신고 접수 시각")
    private LocalDateTime createdAt;

    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getReportId(),
                report.getReason(),
                report.getDetail(),
                report.getStatus(),
                report.getCreatedAt());
    }
}
