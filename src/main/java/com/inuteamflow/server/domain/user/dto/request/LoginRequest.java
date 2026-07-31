package com.inuteamflow.server.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "로그인 요청 DTO")
public class LoginRequest {

    @NotBlank
    @Schema(description = "로그인 아이디", example = "appcenter123")
    private String username;

    @NotBlank
    @Schema(description = "비밀번호", example = "password123!")
    private String password;
}
