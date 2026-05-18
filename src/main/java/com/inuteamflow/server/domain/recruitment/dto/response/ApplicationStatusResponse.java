package com.inuteamflow.server.domain.recruitment.dto.response;

import com.inuteamflow.server.domain.recruitment.entity.RecruitmentApplication;
import com.inuteamflow.server.domain.recruitment.enums.ApplicationStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApplicationStatusResponse {

    private Long applicationID;
    private ApplicationStatus applicationStatus;
    private LocalDateTime respondedAt;

    public static ApplicationStatusResponse from(RecruitmentApplication application) {
        return new ApplicationStatusResponse(
                application.getRecruitmentApplicationId(),
                application.getApplicationStatus(),
                application.getRespondedAt()
        );
    }
}
