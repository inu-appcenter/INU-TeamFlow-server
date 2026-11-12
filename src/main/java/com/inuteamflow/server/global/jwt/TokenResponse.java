package com.inuteamflow.server.global.jwt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.inuteamflow.server.global.enums.ClientType;
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

    // 리프레시 토큰 쿠키의 Max-Age를 정하기 위한 값으로, 응답 본문에는 포함하지 않는다.
    @JsonIgnore
    private ClientType clientType;

    public static TokenResponse of(String accessToken, String refreshToken, ClientType clientType) {
        return new TokenResponse("Bearer", accessToken, refreshToken, clientType);
    }
}
