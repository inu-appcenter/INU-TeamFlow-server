package com.inuteamflow.server.global.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "처리 상태 집계")
public class StatusSummary {

    @Schema(description = "전체 건수", example = "8")
    private long total;

    @Schema(description = "처리 대기 건수", example = "5")
    private long pending;

    @Schema(description = "처리 완료 건수", example = "3")
    private long resolved;

    public static StatusSummary of(long total, long pending, long resolved) {
        return new StatusSummary(total, pending, resolved);
    }
}
