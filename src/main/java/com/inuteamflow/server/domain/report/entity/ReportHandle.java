package com.inuteamflow.server.domain.report.entity;

import com.inuteamflow.server.domain.report.enums.PostActionType;
import com.inuteamflow.server.domain.report.enums.UserActionType;
import com.inuteamflow.server.global.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "report_handle")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportHandle extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_handle_id")
    private Long reportHandleId;

    @OneToOne
    @JoinColumn(name = "report_id")
    private Report report;

    @Column(name = "handler_id")
    private Long handlerId;

    @Column(name = "handler_name")
    private String handlerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_action")
    private PostActionType postAction;

    @Column(name = "post_action_detail")
    private String postActionDetail;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_action")
    private UserActionType userAction;

    @Column(name = "user_action_duration_days")
    private Integer userActionDurationDays;

    @Column(name = "user_action_detail")
    private String userActionDetail;

    @Builder
    private ReportHandle(
            Report report,
            Long handlerId,
            String handlerName,
            PostActionType postAction,
            String postActionDetail,
            UserActionType userAction,
            Integer userActionDurationDays,
            String userActionDetail) {
        this.report = report;
        this.handlerId = handlerId;
        this.handlerName = handlerName;
        this.postAction = postAction;
        this.postActionDetail = postActionDetail;
        this.userAction = userAction;
        this.userActionDurationDays = userActionDurationDays;
        this.userActionDetail = userActionDetail;
    }

    public static ReportHandle create(
            Report report,
            Long handlerId,
            String handlerName,
            PostActionType postAction,
            String postActionDetail,
            UserActionType userAction,
            Integer userActionDurationDays,
            String userActionDetail) {
        return ReportHandle.builder()
                .report(report)
                .handlerId(handlerId)
                .handlerName(handlerName)
                .postAction(postAction)
                .postActionDetail(postActionDetail)
                .userAction(userAction)
                .userActionDurationDays(userActionDurationDays)
                .userActionDetail(userActionDetail)
                .build();
    }
}
