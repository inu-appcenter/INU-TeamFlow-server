package com.inuteamflow.server.domain.vote.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "일정 투표 시간 슬롯 선택 요청 DTO")
public class EventVoteTimeSlotSelectRequest {

    @NotNull
    @Size(min = 1)
    @Schema(description = "선택한 슬롯 ID 목록", example = "[1, 2, 3]")
    private List<@NotNull Long> slotIdList;

}
