package com.inuteamflow.server.domain.fcm.dto.res;

import com.inuteamflow.server.domain.fcm.entity.FcmToken;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "FCM 토큰 응답 DTO")
public class FcmResponse {

    @Schema(description = "서버에 저장된 토큰 ID", example = "14")
    private Long tokenId;

    @Schema(description = "토큰이 저장된 일시", example = "2026-07-13T12:00:00")
    private LocalDateTime createdAt;

    public static FcmResponse create(FcmToken token) {
        return new FcmResponse(token.getFcmTokenId(), token.getCreatedAt());
    }
}
