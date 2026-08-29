package com.inuteamflow.server.domain.recruitment.entity;

import com.inuteamflow.server.global.BaseEntity;
import com.inuteamflow.server.global.enums.Status;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "recruitment_application")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recruitment_application_id")
    private Long recruitmentApplicationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruitment_id", nullable = false)
    private Recruitment recruitment;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String introduction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "application_status")
    private Status applicationStatus;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Builder
    private RecruitmentApplication(Recruitment recruitment, String introduction) {
        this.recruitment = recruitment;
        this.introduction = introduction;
        this.applicationStatus = Status.WAITING;
    }

    public void accept() {
        if (this.applicationStatus != Status.WAITING) {
            throw new RestApiException(CustomErrorCode.RECRUITMENT_APPLICATION_STATUS_INVALID);
        }
        this.applicationStatus = Status.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void decline() {
        if (this.applicationStatus != Status.WAITING) {
            throw new RestApiException(CustomErrorCode.RECRUITMENT_APPLICATION_STATUS_INVALID);
        }
        this.applicationStatus = Status.DECLINED;
        this.respondedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (this.applicationStatus != Status.WAITING) {
            throw new RestApiException(CustomErrorCode.RECRUITMENT_APPLICATION_STATUS_INVALID);
        }
        this.applicationStatus = Status.CANCELLED;
    }
}
