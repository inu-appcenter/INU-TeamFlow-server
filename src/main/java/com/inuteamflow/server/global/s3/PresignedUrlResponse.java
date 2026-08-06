package com.inuteamflow.server.global.s3;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PresignedUrlResponse {

    @NotBlank
    @Schema(description = "S3 업로드용 presigned URL", example = "https://inu-teamflow-images.s3.....Credential=example")
    private String uploadUrl;

    @NotBlank
    @Schema(description = "DB에 저장할 이미지 키", example = "users/profile/550e8400-e29b-41d4-a716-446655440000.png")
    private String imageKey;

    public static PresignedUrlResponse of(String uploadUrl, String imageKey) {
        return new PresignedUrlResponse(uploadUrl, imageKey);
    }
}
