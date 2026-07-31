package com.inuteamflow.server.global.exception.error;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "에러 응답 DTO")
public class ErrorResponse {

    @Schema(description = "에러 코드", example = "400")
    private final Integer code;

    @Schema(description = "에러 메시지", example = "잘못된 요청입니다.")
    private final String message;

    public static ErrorResponse create(Integer code, String message) {
        return new ErrorResponse(code, message);
    }
}
