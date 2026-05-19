package com.inuteamflow.server.domain.user.dto.request;

import com.inuteamflow.server.domain.user.enums.Department;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "회원가입 요청 DTO")
public class SignupRequest {

    @NotBlank
    @Schema(description = "로그인 아이디", example = "appcenter123")
    private String username;

    @NotBlank
    @Schema(description = "비밀번호", example = "password123!")
    private String password;

    @NotBlank
    @Email
    @Schema(description = "이메일", example = "appcenter@inu.ac.kr")
    private String email;

    @NotBlank
    @Schema(description = "이름", example = "홍길동")
    private String name;

    @NotNull
    @Schema(description = "학과", example = "COMPUTER_SCIENCE")
    private Department department;

    @Schema(description = "S3 이미지 Key", example = "users/1/profile.png")
    private String imageKey;

}
