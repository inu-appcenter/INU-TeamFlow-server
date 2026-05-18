package com.inuteamflow.server.domain.recruitment.dto.request;

import com.inuteamflow.server.domain.recruitment.enums.RecruitmentCategory;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentUpdateRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotNull
    private Integer targetMemberCount;

    @NotNull
    private RecruitmentCategory recruitmentCategory;

    @NotNull
    @Future // 현재 시간 이후여야 함
    private LocalDateTime endAt;

}
