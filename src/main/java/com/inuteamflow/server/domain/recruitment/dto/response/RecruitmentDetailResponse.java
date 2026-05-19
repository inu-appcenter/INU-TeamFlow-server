package com.inuteamflow.server.domain.recruitment.dto.response;

import com.inuteamflow.server.domain.recruitment.entity.Recruitment;
import com.inuteamflow.server.global.enums.Category;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RecruitmentDetailResponse {

    private Long recruitmentId;
    private String title;
    private Boolean isOpened;
    private Category category;
    private String description;
    private Integer targetMemberCount;
    private Integer currentMemberCount;
//    private Long announcementId;
//    private String announcementTitle;
    private Long teamId;
    private String teamName;
    private LocalDateTime endAt;
    private Long recruiterId;
    private String recruiterName;
    private Boolean isRecruiter;
    private Boolean hasApplied;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RecruitmentDetailResponse of(Recruitment recruitment,
                                               Boolean isRecruiter,
                                               Boolean hasApplied) {
        return new RecruitmentDetailResponse(
                recruitment.getRecruitmentId(),
                recruitment.getTitle(),
                recruitment.getIsOpened(),
                recruitment.getCategory(),
                recruitment.getDescription(),
                recruitment.getTargetMemberCount(),
                recruitment.getCurrentMemberCount(),
//                recruitment.getAnnouncementId(),
//                announcementTitle,
                recruitment.getTeam().getTeamId(),
                recruitment.getTeam().getName(),
                recruitment.getEndAt(),
                recruitment.getRecruiter().getUserId(),
                recruitment.getRecruiter().getName(),
                isRecruiter,
                hasApplied,
                recruitment.getCreatedAt(),
                recruitment.getUpdatedAt()
        );
    }

}
