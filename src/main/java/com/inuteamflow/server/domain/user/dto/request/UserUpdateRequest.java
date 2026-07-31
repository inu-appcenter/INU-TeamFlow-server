package com.inuteamflow.server.domain.user.dto.request;

import com.inuteamflow.server.domain.user.enums.Department;
import com.inuteamflow.server.domain.user.enums.UserConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "내 정보 수정 요청 DTO")
public class UserUpdateRequest {

    @Schema(description = "변경할 비밀번호", example = "newPassword123!")
    private String password;

    @NotBlank
    @Schema(description = "변경할 이메일", example = "appcenter@inu.ac.kr")
    private String email;

    @NotBlank
    @Schema(description = "변경할 이름", example = "홍길동")
    private String name;

    @NotNull
    @Schema(description = "변경할 학과", example = "COMPUTER_SCIENCE")
    private Department department;

    @Schema(description = "S3 이미지 Key", example = "users/1/profile.png")
    private String imageKey;

    public String getImageKey() {
        return imageKey != null ? imageKey : UserConstants.DEFAULT_PROFILE_KEY;
    }
}
