package com.inuteamflow.server.domain.vote.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "일정 투표 생성 요청 DTO")
public class EventVoteCreateRequest {

    @NotBlank
    @Schema(description = "투표 제목", example = "스터디 일정 조율")
    private String title;

    @NotNull
    @Size(min = 1)
    @Schema(description = "참여자 유저 ID 목록", example = "[1, 2, 3]")
    private List<@NotNull Long> participants;

    @NotNull
    @Schema(description = "종일 여부", example = "false")
    private Boolean isAllDay;

    @NotNull
    @Schema(description = "시작 날짜", example = "2026-05-20")
    private LocalDate startDate;

    @NotNull
    @Schema(description = "종료 날짜", example = "2026-05-25")
    private LocalDate endDate;

    @Schema(description = "시작 시간", example = "18:00:00")
    private LocalTime startTime;

    @Schema(description = "종료 시간", example = "22:00:00")
    private LocalTime endTime;

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "startDate <= endDate")
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) {
            return true;
        }

        return !startDate.isAfter(endDate);
    }

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "isAllDay가 false일 때, startTime과 endTime은 필수이고 startTime < endTime 여야 합니다.")
    public boolean isValidTimeRange() {
        if (isAllDay == null || isAllDay) {
            return true;
        }

        if (startTime == null || endTime == null) {
            return false;
        }

        return startTime.isBefore(endTime);
    }

}
