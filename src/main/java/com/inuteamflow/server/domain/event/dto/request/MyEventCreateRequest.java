package com.inuteamflow.server.domain.event.dto.request;

import com.inuteamflow.server.domain.event.dto.Recurrence;
import com.inuteamflow.server.domain.event.dto.EventCreateCommand;
import com.inuteamflow.server.domain.event.enums.EventColor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "개인 일정 생성 요청 DTO")
public class MyEventCreateRequest implements EventCreateCommand {

    @NotBlank
    @Schema(description = "일정 제목", example = "알고리즘 스터디")
    private String title;

    @Schema(description = "일정 설명", example = "DP 문제 풀이")
    private String description;

    @NotNull
    @Schema(description = "시작 일시", example = "2026-05-20T18:00:00")
    private LocalDateTime startAt;

    @NotNull
    @Schema(description = "종료 일시", example = "2026-05-20T20:00:00")
    private LocalDateTime endAt;

    @NotNull
    @Schema(description = "종일 여부", example = "false")
    private Boolean isAllDay;

    @NotNull
    @Schema(description = "일정 색상", example = "LAVENDER")
    private EventColor color;

    @Valid
    @Schema(description = "반복 일정 규칙")
    private Recurrence recurrence;

    @AssertTrue(message = "startAt < endAt 여야 합니다.")
    public boolean isValidDateRange() {
        if (startAt == null || endAt == null) {
            return true;
        }

        return startAt.isBefore(endAt);
    }
}
