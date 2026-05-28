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

    @Schema(description = "투표 설명", example = "매주 스터디를 진행할 수 있는 요일을 선택해주세요.")
    private String description;

    @NotNull
    @Size(min = 1)
    @Schema(description = "참여자 유저 ID 목록", example = "[1, 2, 3]")
    private List<@NotNull Long> participants;

    @NotNull
    @Schema(description = "종일 여부", example = "false")
    private Boolean isAllDay;

    @NotNull
    @Size(min = 1)
    @Schema(description = "지정된 투표 날짜", example = "[\"2026-05-20\", \"2026-05-21\", \"2026-05-25\"]")
    private List<@NotNull LocalDate> dates;

    @Schema(description = "일일 시작 시간", example = "09:00:00")
    private LocalTime dailyTimeStart;

    @Schema(description = "일일 종료 시간", example = "20:00:00")
    private LocalTime dailyTimeEnd;

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "isAllDay가 false일 때, dailyTimeStart과 dailyTimeEnd은 필수이고 dailyTimeStart < dailyTimeEnd 여야 합니다.")
    public boolean isValidTimeRange() {
        if (isAllDay == null || isAllDay) {
            return true;
        }

        if (dailyTimeStart == null || dailyTimeEnd == null) {
            return false;
        }

        return dailyTimeStart.isBefore(dailyTimeEnd);
    }

}
