package com.inuteamflow.server.domain.push.dto.req;

import com.inuteamflow.server.domain.push.enums.PushProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "푸시 토큰 저장 요청 DTO")
public class PushTokenRequest {

    @NotBlank
    @Schema(description = "Firebase SDK 또는 Expo로부터 받은 푸시 토큰", example = "eXaMpLeF1c7oKeN...:APA91bF...")
    private String token;

    @NotBlank
    @Schema(description = "디바이스 타입", example = "web")
    private String deviceType;

    @Schema(description = "토큰 발급 제공자 (미입력 시 FCM)", example = "FCM")
    private PushProvider provider;
}
