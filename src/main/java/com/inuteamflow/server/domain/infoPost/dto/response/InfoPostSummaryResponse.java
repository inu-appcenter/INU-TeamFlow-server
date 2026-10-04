package com.inuteamflow.server.domain.infoPost.dto.response;

import com.inuteamflow.server.domain.infoPost.entity.InfoPost;
import com.inuteamflow.server.domain.infoPost.enums.InfoPostCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
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

    @Schema(
            description = "본문 미리보기 (앞 100자, 줄바꿈은 공백으로 치환)",
            example = "인천대학교 학생을 대상으로 2026 INU 가나디 공모전을 개최합니다. 많은 참여 바랍니다.")
    private String preview;

    @Schema(description = "썸네일 이미지 URL")
    private String thumbnailUrl;

    @Schema(description = "이 정보글을 참조하는 모집글 수 (공고형만 값, 자유형은 null)", example = "4")
    private Integer recruitmentCount;

    @Schema(description = "작성일시")
    private LocalDateTime createdAt;

    private static final int PREVIEW_LENGTH = 100;

    public static InfoPostSummaryResponse of(InfoPost infoPost, String thumbnailUrl, Integer recruitmentCount) {
        return new InfoPostSummaryResponse(
                infoPost.getInfoPostId(),
                infoPost.getCategory(),
                infoPost.isLinkable(),
                infoPost.getTitle(),
                toPreview(infoPost.getContent()),
                thumbnailUrl,
                recruitmentCount,
                infoPost.getCreatedAt());
    }

    /**
     * 본문을 목록용 미리보기로 변환한다.
     *
     * <p>줄바꿈/연속 공백을 공백 하나로 합친 뒤 앞 {@value #PREVIEW_LENGTH}자만 남긴다. 이모지 같은 서로게이트 쌍이 중간에 잘리지 않도록 code point 기준으로 자른다.
     */
    private static String toPreview(String content) {
        if (content == null) {
            return null;
        }
        String normalized = content.replaceAll("\\s+", " ").strip();
        if (normalized.codePointCount(0, normalized.length()) <= PREVIEW_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, normalized.offsetByCodePoints(0, PREVIEW_LENGTH));
    }
}
