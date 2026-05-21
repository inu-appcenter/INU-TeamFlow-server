package com.inuteamflow.server.domain.vote.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "일정 투표 최종 시간 선택 요청 DTO")
public class EventVoteTimeSelectRequest {

    @Schema(description = "생성할 일정 제목", example = "알고리즘 스터디")
    private String title;

    @NotNull
    @Schema(description = "종일 여부", example = "false")
    private Boolean isAllDay;

    @NotNull
    @Schema(description = "선택된 시작 일시", example = "2026-05-20T18:00:00")
    private LocalDateTime selectedStartAt;

    @NotNull
    @Schema(description = "선택된 종료 일시", example = "2026-05-20T20:00:00")
    private LocalDateTime selectedEndAt;

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "selectedStartAt < selectedEndAt 여야 합니다")
    public boolean isValidDateTimeRange() {
        if (selectedStartAt == null || selectedEndAt == null) {
            return true;
        }

        return selectedStartAt.isBefore(selectedEndAt);
    }

}
