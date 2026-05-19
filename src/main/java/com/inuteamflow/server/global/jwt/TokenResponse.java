package com.inuteamflow.server.global.jwt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "JWT 토큰 발급 응답 DTO")
public class TokenResponse {

    @Schema(description = "인증 타입", example = "Bearer")
    private String grantType;

    @Schema(description = "Access 토큰", example = "eyJhbGciOiJIUzI1NiJ9.eyJ.....")
    private String accessToken;

    @JsonIgnore
    private String refreshToken;
}
