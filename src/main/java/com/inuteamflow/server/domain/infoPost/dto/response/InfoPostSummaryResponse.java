package com.inuteamflow.server.domain.infoPost.dto.response;

import com.inuteamflow.server.domain.infoPost.entity.InfoPost;
import com.inuteamflow.server.domain.infoPost.enums.InfoPostCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "정보글 요약 응답 DTO")
public class InfoPostSummaryResponse {

    @Schema(description = "정보글 ID", example = "3")
    private Long infoPostId;

    @Schema(description = "카테고리", example = "CONTEST")
    private InfoPostCategory category;

    @Schema(description = "모집글 연결 가능 여부", example = "true")
    private Boolean linkable;

    @Schema(description = "제목", example = "2026 INU 가나디 공모전")
    private String title;

    @Schema(description = "썸네일 이미지 URL")
    private String thumbnailUrl;

    @Schema(description = "이 정보글을 참조하는 모집글 수 (공고형만 값, 자유형은 null)", example = "4")
    private Integer recruitmentCount;

    public static InfoPostSummaryResponse of(InfoPost infoPost, String thumbnailUrl, Integer recruitmentCount) {
        return new InfoPostSummaryResponse(
                infoPost.getInfoPostId(),
                infoPost.getCategory(),
                infoPost.isLinkable(),
                infoPost.getTitle(),
                thumbnailUrl,
                recruitmentCount
        );
    }
}
