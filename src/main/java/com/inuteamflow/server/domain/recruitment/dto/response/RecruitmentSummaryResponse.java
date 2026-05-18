package com.inuteamflow.server.domain.recruitment.dto.response;

import com.inuteamflow.server.domain.recruitment.entity.Recruitment;
import com.inuteamflow.server.domain.recruitment.enums.RecruitmentCategory;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RecruitmentSummaryResponse {

    private Long recruitmentId;
    private String title;
    private Boolean isOpened;
    private RecruitmentCategory category;
//    private String announcementTitle;
    private String recruiterName;
    private LocalDateTime createdAt;
    private LocalDateTime endAt;

    public static RecruitmentSummaryResponse from(Recruitment recruitment) {
        return new RecruitmentSummaryResponse(
                recruitment.getRecruitmentId(),
                recruitment.getTitle(),
                recruitment.getIsOpened(),
                recruitment.getCategory(),
//                announcementTitle,
                recruitment.getRecruiter().getName(),
                recruitment.getCreatedAt(),
                recruitment.getEndAt()
        );
    }

}
