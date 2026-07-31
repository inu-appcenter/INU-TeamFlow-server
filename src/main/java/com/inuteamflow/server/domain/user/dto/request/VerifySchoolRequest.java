package com.inuteamflow.server.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "학교 포털 인증 요청 DTO")
public class VerifySchoolRequest {

    @NotBlank
    @Schema(description = "학번", example = "202201234")
    private String studentNumber;

    @NotBlank
    @Schema(description = "포털 비밀번호", example = "portalPassword123!")
    private String portalPassword;
}
