package com.inuteamflow.server.domain.recruitment.dto.response;

import com.inuteamflow.server.domain.recruitment.entity.RecruitmentApplication;
import com.inuteamflow.server.global.enums.Category;
import com.inuteamflow.server.global.enums.Status;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MyApplicationSummaryResponse {

    private Long applicationId;
    private Status applicationStatus;
    private Category recruitmentCategory;
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
