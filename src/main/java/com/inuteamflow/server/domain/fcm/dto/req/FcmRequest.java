package com.inuteamflow.server.domain.fcm.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "FCM 토큰 저장 요청 DTO")
public class FcmRequest {

    @NotBlank
    @Schema(description = "Firebase SDK 로부터 받은 토큰", example = "eXaMpLeF1c7oKeN...:APA91bF...")
    private String token;

    @NotBlank
    @Schema(description = "디바이스 타입", example = "web")
    private String deviceType;
}
