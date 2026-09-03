package com.inuteamflow.server.domain.event.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.inuteamflow.server.domain.event.entity.RecurrenceRule;
import com.inuteamflow.server.domain.event.enums.RecurrenceFrequency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "반복 일정 규칙 DTO")
public class Recurrence {

    @NotNull
    @Schema(description = "반복 주기", example = "WEEKLY")
    private RecurrenceFrequency freq;

    @NotNull
    @Positive
    @Schema(description = "반복 간격", example = "2")
    private Integer intervalValue;

    @Schema(description = "반복 요일 목록", example = "[\"MONDAY\", \"WEDNESDAY\"]")
    private List<DayOfWeek> byDay;

    @Min(1)
    @Max(31)
    @Schema(description = "월 반복 기준 일자", example = "15")
    private Integer byMonthDay;

    @Schema(description = "반복 시작 일시", example = "null")
    private LocalDateTime seriesStartAt;

    @Schema(description = "반복 종료 일시", example = "2026-06-20T18:00:00")
    private LocalDateTime untilAt;

    @Positive
    @Schema(description = "반복 횟수", example = "10")
    private Integer occurrenceCount;

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "untilAt and occurrenceCount cannot be used together")
    public boolean isValidEndCondition() {
        return untilAt == null || occurrenceCount == null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "byDay is required for weekly recurrence")
    public boolean isValidWeeklyByDay() {
        if (freq != RecurrenceFrequency.WEEKLY) {
            return true;
        }

        return byDay != null && !byDay.isEmpty();
    }

    public static Recurrence from(RecurrenceRule recurrenceRule) {
        if (recurrenceRule == null) {
            return null;
        }

        return new Recurrence(
                recurrenceRule.getFreq(),
                recurrenceRule.getIntervalValue(),
                // LAZY @ElementCollection을 트랜잭션 내에서 초기화하여 직렬화 시 LazyInitializationException을 방지
                new ArrayList<>(recurrenceRule.getByDay()),
                recurrenceRule.getByMonthDay(),
                recurrenceRule.getSeriesStartAt(),
                recurrenceRule.getUntilAt(),
                recurrenceRule.getOccurrenceCount());
    }
}
