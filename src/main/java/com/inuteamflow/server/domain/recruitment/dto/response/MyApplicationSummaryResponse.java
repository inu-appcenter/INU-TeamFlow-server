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
public class MyApplicationSummaryResponse {

    private Long applicationId;
    private ApplicationStatus applicationStatus;
    private RecruitmentCategory recruitmentCategory;
    private String recruiterName;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;


    public static MyApplicationSummaryResponse from(RecruitmentApplication application) {
        return new MyApplicationSummaryResponse(
                application.getRecruitmentApplicationId(),
                application.getApplicationStatus(),
                application.getRecruitment().getCategory(),
                application.getRecruitment().getRecruiter().getName(),
                application.getCreatedAt(),
                application.getRespondedAt()
        );
    }
}
