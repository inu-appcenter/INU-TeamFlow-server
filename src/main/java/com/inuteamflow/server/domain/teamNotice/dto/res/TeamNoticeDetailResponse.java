package com.inuteamflow.server.domain.teamNotice.dto.res;

import com.inuteamflow.server.domain.teamNotice.entity.TeamNotice;
import com.inuteamflow.server.domain.teamNotice.entity.TeamNoticeImage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "팀 공지 상세 응답 DTO")
public class TeamNoticeDetailResponse {

    @Schema(description = "공지 ID", example = "1")
    private Long noticeId;

    @Schema(description = "공지 제목", example = "5월 정기 회의 안내")
    private String title;

    @Schema(description = "상단 고정 여부", example = "true")
    private Boolean isPinned;

    @Schema(description = "작성자 정보")
    private TeamNoticeAuthorResponse author;

    @Schema(description = "작성일시", example = "2026-06-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2026-06-02T15:30:00")
    private LocalDateTime updatedAt;

    @Schema(description = "공지 이미지 목록")
    private List<Image> images;

    @Schema(description = "공지 내용", example = "이번 달 정기 회의는 5월 30일 오전 10시에 진행됩니다.")
    private String content;

    @Schema(description = "수정/삭제 가능 여부", example = "true")
    private Boolean isEditable;

    public static TeamNoticeDetailResponse of(
            TeamNotice notice,
            TeamNoticeAuthorResponse author,
            List<TeamNoticeImage> images,
            Function<String, String> urlResolver,
            boolean isEditable) {
        List<Image> imageList = images.stream()
                .map(img -> new Image(urlResolver.apply(img.getImageKey()), img.getSortOrder()))
                .toList();

        return new TeamNoticeDetailResponse(
                notice.getTeamNoticeId(),
                notice.getTitle(),
                notice.getIsPinned(),
                author,
                notice.getCreatedAt(),
                notice.getUpdatedAt(),
                imageList,
                notice.getContent(),
                isEditable);
    }

    @Schema(description = "공지 이미지")
    private record Image(
            @Schema(description = "이미지 URL") String imageUrl,

            @Schema(description = "이미지 정렬 순서", example = "0")
            Integer sortOrder) {}
}
