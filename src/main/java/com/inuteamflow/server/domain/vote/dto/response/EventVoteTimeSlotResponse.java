package com.inuteamflow.server.domain.vote.dto.response;

import com.inuteamflow.server.domain.vote.entity.VoteTimeSlot;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "일정 투표 시간 슬롯 응답 DTO")
public class EventVoteTimeSlotResponse {

    @Schema(description = "슬롯 ID", example = "1")
    private Long slotId;

    @Schema(description = "날짜", example = "2026-05-20")
    private LocalDate date;

    @Schema(description = "시작 시간", example = "18:00:00")
    private LocalTime startAt;

    @Schema(description = "종료 시간", example = "19:00:00")
    private LocalTime endAt;

    @Schema(description = "참여 인원 수", example = "5")
    private Integer participantCount;

    public static EventVoteTimeSlotResponse create(
            VoteTimeSlot voteTimeSlot,
            Integer participantCount
    ) {
        return new EventVoteTimeSlotResponse(
                voteTimeSlot.getVoteTimeSlotId(),
                voteTimeSlot.getVoteDate().getDate(),
                voteTimeSlot.getSlotStartAt(),
                voteTimeSlot.getSlotEndAt(),
                participantCount
        );
    }
}
