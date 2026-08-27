package com.inuteamflow.server.global.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "게시글 참조 DTO")
public class PostRef {

    @Schema(description = "게시글 ID", example = "5")
    private Long postId;

    @Schema(description = "게시글 제목", example = "정보글 제목")
    private String title;

    public static PostRef of(Long postId, String title) {
        return new PostRef(postId, title);
    }
}
