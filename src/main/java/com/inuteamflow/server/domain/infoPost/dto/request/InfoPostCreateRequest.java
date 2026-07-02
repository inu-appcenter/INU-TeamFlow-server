package com.inuteamflow.server.domain.infoPost.dto.request;

import com.inuteamflow.server.global.enums.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "정보글 생성 요청 DTO")
public class InfoPostCreateRequest {

    @NotNull
    @Schema(description = "카테고리", example = "CONTEST")
    private Category category;

    @NotBlank
    @Schema(description = "제목", example = "2026 INU 가나디 공모전")
    private String title;

    @NotBlank
    @Schema(description = "내용", example = "어쩌구 저쩌구")
    private String content;

    @Schema(description = "업로드할 이미지의 S3 키 목록 (presigned URL 요청에서 받은 값)",
            example = "[\"info-posts/image/550e8...\"]")
    private List<String> imageKeys;
}
