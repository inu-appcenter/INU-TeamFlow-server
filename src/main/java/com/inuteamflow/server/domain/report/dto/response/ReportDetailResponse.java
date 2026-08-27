package com.inuteamflow.server.domain.report.dto.response;

import com.inuteamflow.server.domain.report.entity.Report;
import com.inuteamflow.server.domain.report.entity.ReportHandle;
import com.inuteamflow.server.domain.report.enums.PostActionType;
import com.inuteamflow.server.domain.report.enums.ReportReason;
import com.inuteamflow.server.domain.report.enums.ReportStatus;
import com.inuteamflow.server.domain.report.enums.ReportTargetType;
import com.inuteamflow.server.domain.report.enums.UserActionType;
import com.inuteamflow.server.global.dto.PostRef;
import com.inuteamflow.server.global.dto.UserRef;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "신고 상세 응답 DTO (관리자)")
public class ReportDetailResponse {

    @Schema(description = "신고 ID", example = "12")
    private Long reportId;

    @Schema(description = "신고 사유", example = "ABUSE")
    private ReportReason reason;

    @Schema(description = "상세 내용")
    private String detail;

    @Schema(description = "신고 대상 유형", example = "INFO_POST")
    private ReportTargetType targetType;

    @Schema(description = "처리 상태", example = "PENDING")
    private ReportStatus status;

    @Schema(description = "신고자")
    private UserRef reporter;

    @Schema(description = "대상 게시글 (USER 신고면 null)")
    private PostRef targetPost;

    @Schema(description = "대상 사용자 (게시글 신고면 작성자)")
    private UserRef targetUser;

    @Schema(description = "게시글 조치 (PENDING이면 null)")
    private PostActionView postAction;

    @Schema(description = "사용자 조치 (PENDING이면 null)")
    private UserActionView userAction;

    @Schema(description = "처리한 관리자 (PENDING이면 null)")
    private UserRef handledBy;

    @Schema(description = "처리 시각 (PENDING이면 null)")
    private LocalDateTime handledAt;

    @Schema(description = "신고 접수 시각")
    private LocalDateTime createdAt;

    public static ReportDetailResponse of(Report report, ReportHandle handle) {
        boolean handled = handle != null;

        return new ReportDetailResponse(
                report.getReportId(),
                report.getReason(),
                report.getDetail(),
                report.getTargetType(),
                report.getStatus(),
                UserRef.of(report.getReporterId(), report.getReporterName()),
                report.getTargetType() == ReportTargetType.USER
                        ? null
                        : PostRef.of(report.getTargetPostId(), report.getTargetPostTitle()),
                report.getTargetUserId() == null
                        ? null
                        : UserRef.of(report.getTargetUserId(), report.getTargetUserName()),
                handled ? PostActionView.from(handle) : null,
                handled ? UserActionView.from(handle) : null,
                handled ? UserRef.of(handle.getHandlerId(), handle.getHandlerName()) : null,
                handled ? handle.getCreatedAt() : null,
                report.getCreatedAt());
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Schema(description = "게시글 조치")
    public static class PostActionView {

        @Schema(description = "조치 유형", example = "DELETE")
        private PostActionType action;

        @Schema(description = "조치 사유")
        private String detail;

        private static PostActionView from(ReportHandle handle) {
            if (handle.getPostAction() == null) {
                return null;
            }
            return new PostActionView(handle.getPostAction(), handle.getPostActionDetail());
        }
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Schema(description = "사용자 조치")
    public static class UserActionView {

        @Schema(description = "조치 유형", example = "SUSPEND")
        private UserActionType action;

        @Schema(description = "정지 일수 (SUSPEND가 아니면 null)", example = "30")
        private Integer durationDays;

        @Schema(description = "조치 사유")
        private String detail;

        private static UserActionView from(ReportHandle handle) {
            if (handle.getUserAction() == null) {
                return null;
            }
            return new UserActionView(
                    handle.getUserAction(), handle.getUserActionDurationDays(), handle.getUserActionDetail());
        }
    }
}
