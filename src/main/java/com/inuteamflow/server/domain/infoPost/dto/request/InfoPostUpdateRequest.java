package com.inuteamflow.server.domain.infoPost.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "정보글 수정 요청 DTO")
public class InfoPostUpdateRequest {

    @NotBlank
    @Schema(description = "제목", example = "2026 가나디 공모전 (수정)")
    private String title;

    @NotBlank
    @Schema(description = "내용", example = "어쩌구 저쩌구 블라블라")
    private String content;

    @Schema(description = "수정된 이미지 순서대로의 S3 키 목록 (기존 이미지 교체 시 새 키로 전달)",
            example = "[\"info-posts/image/550e8...\"]")
    private List<String> imageKeys;
}
