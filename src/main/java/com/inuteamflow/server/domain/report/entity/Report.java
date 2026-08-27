package com.inuteamflow.server.domain.report.entity;

import com.inuteamflow.server.domain.report.enums.ReportReason;
import com.inuteamflow.server.domain.report.enums.ReportStatus;
import com.inuteamflow.server.domain.report.enums.ReportTargetType;
import com.inuteamflow.server.global.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "report")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "reporter_name", nullable = false)
    private String reporterName;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private ReportTargetType targetType;

    @Column(name = "target_post_id")
    private Long targetPostId;

    @Column(name = "target_post_title")
    private String targetPostTitle;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "target_user_name")
    private String targetUserName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

    @Column(length = 1000)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @Builder
    private Report(
            Long reporterId,
            String reporterName,
            ReportTargetType targetType,
            Long targetPostId,
            String targetPostTitle,
            Long targetUserId,
            String targetUserName,
            ReportReason reason,
            String detail) {
        this.reporterId = reporterId;
        this.reporterName = reporterName;
        this.targetType = targetType;
        this.targetPostId = targetPostId;
        this.targetPostTitle = targetPostTitle;
        this.targetUserId = targetUserId;
        this.targetUserName = targetUserName;
        this.reason = reason;
        this.detail = detail;
        this.status = ReportStatus.PENDING;
    }

    public static Report create(
            Long reporterId,
            String reporterName,
            ReportTargetType targetType,
            Long targetPostId,
            String targetPostTitle,
            Long targetUserId,
            String targetUserName,
            ReportReason reason,
            String detail) {
        return Report.builder()
                .reporterId(reporterId)
                .reporterName(reporterName)
                .targetType(targetType)
                .targetPostId(targetPostId)
                .targetPostTitle(targetPostTitle)
                .targetUserId(targetUserId)
                .targetUserName(targetUserName)
                .reason(reason)
                .detail(detail)
                .build();
    }

    public void resolve() {
        this.status = ReportStatus.RESOLVED;
    }
}
