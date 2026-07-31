package com.inuteamflow.server.global.s3;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "Presigned URL 발급 요청 DTO")
public class PresignedUrlRequest {

    @NotBlank
    @Schema(description = "원본 파일명", example = "profile-image.png")
    private String fileName;

    @NotBlank
    @Schema(description = "이미지의 MIME 타입", example = "image/png")
    private String contentType;
}
