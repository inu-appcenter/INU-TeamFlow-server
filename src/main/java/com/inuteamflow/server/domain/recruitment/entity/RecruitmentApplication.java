package com.inuteamflow.server.domain.recruitment.entity;

import com.inuteamflow.server.domain.recruitment.enums.ApplicationStatus;
import com.inuteamflow.server.global.BaseEntity;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private ApplicationStatus applicationStatus;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Builder
    private RecruitmentApplication(Recruitment recruitment, String introduction) {
        this.recruitment = recruitment;
        this.introduction = introduction;
        this.applicationStatus = ApplicationStatus.WAITING;
    }

    public void accept() {
        if (this.applicationStatus != ApplicationStatus.WAITING) {
            throw new RestApiException(CustomErrorCode.RECRUITMENT_APPLICATION_STATUS_INVALID);
        }
        this.applicationStatus = ApplicationStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void decline() {
        if (this.applicationStatus != ApplicationStatus.WAITING) {
            throw new RestApiException(CustomErrorCode.RECRUITMENT_APPLICATION_STATUS_INVALID);
        }
        this.applicationStatus = ApplicationStatus.DECLINED;
        this.respondedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (this.applicationStatus != ApplicationStatus.WAITING) {
            throw new RestApiException(CustomErrorCode.RECRUITMENT_APPLICATION_STATUS_INVALID);
        }
        this.applicationStatus = ApplicationStatus.CANCELED;
    }

}
