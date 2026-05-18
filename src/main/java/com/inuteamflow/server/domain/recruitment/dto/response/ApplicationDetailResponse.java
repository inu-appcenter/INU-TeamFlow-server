package com.inuteamflow.server.domain.recruitment.dto.response;

import com.inuteamflow.server.domain.recruitment.entity.RecruitmentApplication;
import com.inuteamflow.server.domain.recruitment.enums.ApplicationStatus;
import com.inuteamflow.server.domain.recruitment.enums.RecruitmentCategory;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApplicationDetailResponse {

    private Long applicationId;
    private ApplicationStatus applicationStatus;
    private String introduction;
    private String recruitmentTitle;
    private RecruitmentCategory category;
    private String recruiterName;
    //    private String announcementTitle;
    private String applicantName;
    private Boolean isRecruiter;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    public static ApplicationDetailResponse of(RecruitmentApplication application,
                                               String applicantName,
                                               Boolean isRecruiter) {
        return new ApplicationDetailResponse(
                application.getRecruitmentApplicationId(),
                application.getApplicationStatus(),
                application.getIntroduction(),
                application.getRecruitment().getTitle(),
                application.getRecruitment().getCategory(),
                application.getRecruitment().getRecruiter().getName(),
//                announcementTitle,
                applicantName,
                isRecruiter,
                application.getCreatedAt(),
                application.getRespondedAt()
        );
    }
}
