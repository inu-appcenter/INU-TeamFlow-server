package com.inuteamflow.server.domain.recruitment.dto.response;

import com.inuteamflow.server.domain.recruitment.entity.RecruitmentApplication;
import com.inuteamflow.server.global.enums.Status;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApplicationSummaryResponse {

    private Long applicationId;
    private String introduction;
    private Status applicationStatus;
    private String applicantName;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    public static ApplicationSummaryResponse of(RecruitmentApplication application, String applicantName) {
        return new ApplicationSummaryResponse(
                application.getRecruitmentApplicationId(),
                application.getIntroduction(),
                application.getApplicationStatus(),
                applicantName,
                application.getCreatedAt(),
                application.getRespondedAt()
        );
    }

}
