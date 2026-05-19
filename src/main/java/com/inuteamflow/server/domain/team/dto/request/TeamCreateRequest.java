package com.inuteamflow.server.domain.team.dto.request;

import com.inuteamflow.server.global.enums.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamCreateRequest {

    @NotBlank
    private String name;

    @NotNull
    private Category category;

    @NotNull
    private String description;

    private String link;

    private String sns;

    private String imageKey;
}
