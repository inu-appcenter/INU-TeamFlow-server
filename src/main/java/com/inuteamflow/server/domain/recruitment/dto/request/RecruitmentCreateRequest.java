package com.inuteamflow.server.domain.recruitment.dto.request;

import com.inuteamflow.server.global.enums.Category;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentCreateRequest {

    @NotBlank
    private String title;

    @NotNull
    private Category category;

    @NotBlank
    private String description;

    @NotNull
    private Long teamId;

    @NotNull
    private Integer targetMemberCount;

    @NotNull
    @Future // 현재 시간 이후여야 함
    private LocalDateTime endAt;

}
