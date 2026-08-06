package com.inuteamflow.server.domain.event.dto.response;

import com.inuteamflow.server.domain.event.dto.Participant;
import com.inuteamflow.server.domain.event.dto.Recurrence;
import com.inuteamflow.server.domain.event.entity.Event;
import com.inuteamflow.server.domain.event.entity.RecurrenceException;
import com.inuteamflow.server.domain.event.entity.RecurrenceRule;
import com.inuteamflow.server.domain.event.enums.EventColor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "일정 목록 응답 DTO")
public class EventListResponse {

    @Schema(description = "일정 ID", example = "1")
    private Long eventId;

    @Schema(description = "팀 ID", example = "10")
    private Long teamId;

    @Schema(description = "팀 이름", example = "앱센터")
    private String teamName;

    @Schema(description = "일정 제목", example = "팀 회의")
    private String title;

    @Schema(description = "일정 설명", example = "프로젝트 진행 상황 공유")
    private String description;

    @Schema(description = "반복 일정 발생 일시", example = "2026-05-20T18:00:00")
    private LocalDateTime occurrenceAt;

    @Schema(description = "시작 일시", example = "2026-05-20T18:00:00")
    private LocalDateTime startAt;

    @Schema(description = "종료 일시", example = "2026-05-20T20:00:00")
    private LocalDateTime endAt;

    @Schema(description = "종일 여부", example = "false")
    private Boolean isAllDay;

    @Schema(description = "일정 색상", example = "LAVENDER")
    private EventColor color;

    @Schema(description = "단일 일정 여부", example = "true")
    private Boolean isSingle;

    @Schema(description = "완료 여부", example = "false")
    private Boolean isFinished;

    @Schema(description = "반복 일정 예외 여부", example = "false")
    private Boolean isException;

    @Schema(description = "요청자의 참석자 여부", example = "true")
    private Boolean isParticipant;

    @Schema(description = "팀 일정 참석자")
    private List<Participant> participants;

    @Schema(description = "반복 일정 규칙")
    private Recurrence recurrence;

    public static EventListResponse of(Event event, boolean isParticipant, List<Participant> participants) {
        return new EventListResponse(
                event.getEventId(),
                event.getTeamId(),
                event.getTeamId() != null ? event.getTeam().getName() : null,
                event.getTitle(),
                event.getDescription(),
                null,
                event.getStartAt(),
                event.getEndAt(),
                event.getIsAllDay(),
                event.getColor(),
                event.getIsSingle(),
                event.getIsFinished(),
                false,
                isParticipant,
                participants,
                null // 단건 일정임, RecurrenceRule 없음
                );
    }

    public static EventListResponse of(
            Event event,
            RecurrenceRule recurrenceRule,
            LocalDateTime occurrenceAt,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean isParticipant,
            List<Participant> participants) {
        return new EventListResponse(
                event.getEventId(),
                event.getTeamId(),
                event.getTeamId() != null ? event.getTeam().getName() : null,
                event.getTitle(),
                event.getDescription(),
                occurrenceAt,
                startAt,
                endAt,
                event.getIsAllDay(),
                event.getColor(),
                event.getIsSingle(),
                event.getIsFinished(),
                false,
                isParticipant,
                participants,
                Recurrence.from(recurrenceRule));
    }

    public static EventListResponse of(
            Event event,
            RecurrenceRule recurrenceRule,
            RecurrenceException recurrenceException,
            boolean isParticipant,
            List<Participant> participants) {
        return new EventListResponse(
                event.getEventId(),
                event.getTeamId(),
                event.getTeamId() != null ? event.getTeam().getName() : null,
                recurrenceException.getModifiedTitle(),
                recurrenceException.getModifiedDescription(),
                recurrenceException.getOriginalOccurrenceAt(),
                recurrenceException.getModifiedStartAt(),
                recurrenceException.getModifiedEndAt(),
                recurrenceException.getModifiedIsAllDay(),
                recurrenceException.getModifiedColor(),
                event.getIsSingle(),
                recurrenceException.getModifiedIsFinished(),
                true,
                isParticipant,
                participants,
                Recurrence.from(recurrenceRule));
    }
}
