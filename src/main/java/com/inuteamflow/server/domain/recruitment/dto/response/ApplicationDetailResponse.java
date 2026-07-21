package com.inuteamflow.server.domain.recruitment.dto.response;

import com.inuteamflow.server.domain.recruitment.entity.RecruitmentApplication;
import com.inuteamflow.server.domain.user.enums.Department;
import com.inuteamflow.server.global.enums.Category;
import com.inuteamflow.server.global.enums.Status;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApplicationDetailResponse {

    private Long applicationId;
    private Status applicationStatus;
    private String introduction;
    private Long teamId;
    private String recruitmentTitle;
    private Category category;
    private String recruiterName;
    private String applicantName;
    private Department applicantDepartment;
    private String applicantStudentNumber;
    private Boolean isRecruiter;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    public static ApplicationDetailResponse of(RecruitmentApplication application,
                                               String applicantName,
                                               Department applicantDepartment,
                                               String applicantStudentNumber,
                                               Boolean isRecruiter) {
        return new ApplicationDetailResponse(
                application.getRecruitmentApplicationId(),
                application.getApplicationStatus(),
                application.getIntroduction(),
                application.getRecruitment().getTeam().getTeamId(),
                application.getRecruitment().getTitle(),
                application.getRecruitment().getCategory(),
                application.getRecruitment().getRecruiter().getName(),
                applicantName,
                applicantDepartment,
                applicantStudentNumber,
                isRecruiter,
                application.getCreatedAt(),
                application.getRespondedAt()
        );
    }
}