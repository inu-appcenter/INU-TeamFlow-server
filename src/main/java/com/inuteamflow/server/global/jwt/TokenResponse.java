package com.inuteamflow.server.global.jwt;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "JWT 토큰 발급 응답 DTO")
public class TokenResponse {

    @Schema(description = "인증 타입", example = "Bearer")
    private String grantType;

    @Schema(description = "Access 토큰", example = "eyJhbGciOiJIUzI1NiJ9.eyJ.....")
    private String accessToken;

    @Schema(description = "Refresh 토큰", example = "eyJhbGciOiJIUzI1NiJ9.eyJ.....")
    private String refreshToken;

    public static TokenResponse of(String accessToken, String refreshToken) {
        return new TokenResponse("Bearer", accessToken, refreshToken);
    }
}
