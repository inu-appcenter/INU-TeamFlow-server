package com.inuteamflow.server.domain.event.dto.request;

import com.inuteamflow.server.domain.event.dto.Recurrence;
import com.inuteamflow.server.domain.event.dto.EventUpdateCommand;
import com.inuteamflow.server.domain.event.enums.EventColor;
import com.inuteamflow.server.domain.event.enums.RecurrenceEditScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "팀 일정 수정 요청 DTO")
public class TeamEventUpdateRequest implements EventUpdateCommand {

    @NotBlank
    @Schema(description = "일정 제목", example = "팀 회의")
    private String title;

    @Schema(description = "일정 설명", example = "프로젝트 진행 상황 공유")
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

    @Schema(description = "완료 여부", example = "true")
    private Boolean isFinished;

    @Schema(description = "참여자 멤버 ID 목록", example = "[1, 2, 3]")
    private List<@NotNull Long> participants;

    @Schema(description = "반복 일정 수정 범위", example = "THIS_INSTANCE")
    private RecurrenceEditScope recurrenceEditScope;

    @Schema(description = "수정 대상 반복 일정 발생 일시", example = "2026-05-20T18:00:00")
    private LocalDateTime occurrenceAt;

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

    @AssertTrue(message = "occurrenceAt은 THIS_INSTANCE 혹은 THIS_AND_FOLLOWING가 필요합니다.")
    public boolean isValidOccurrenceAt() {
        if (recurrenceEditScope == RecurrenceEditScope.THIS_INSTANCE
                || recurrenceEditScope == RecurrenceEditScope.THIS_AND_FOLLOWING) {
            return occurrenceAt != null;
        }

        return true;
    }
}
