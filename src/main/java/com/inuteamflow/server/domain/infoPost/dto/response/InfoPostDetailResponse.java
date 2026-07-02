package com.inuteamflow.server.domain.infoPost.dto.response;

import com.inuteamflow.server.domain.infoPost.entity.InfoPost;
import com.inuteamflow.server.domain.infoPost.entity.InfoPostImage;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.global.enums.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.time.LocalDateTime;
import java.util.function.Function;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "정보글 상세 응답 DTO")
public class InfoPostDetailResponse {

    @Schema(description = "정보글 ID", example = "3")
    private Long infoPostId;

    @Schema(description = "카테고리", example = "CONTEST")
    private Category category;

    @Schema(description = "모집글 연결 가능 여부", example = "true")
    private Boolean linkable;

    @Schema(description = "제목", example = "2026 INU 가나디 공모전")
    private String title;

    @Schema(description = "내용", example = "어쩌구 저쩌구")
    private String content;

    @Schema(description = "이미지 목록")
    private List<Image> images;

    @Schema(description = "작성자 정보")
    private Author author;

    @Schema(description = "수정/삭제 버튼 노출 판단용", example = "true")
    private Boolean isAuthor;

    @Schema(description = "이 정보글을 참조하는 모집글 수 (공고형만 값, 자유형은 null)", example = "4")
    private Integer recruitmentCount;

    @Schema(description = "작성일시", example = "2026-05-20T18:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2026-05-25T23:00:00")
    private LocalDateTime updatedAt;

    public static InfoPostDetailResponse of(
            InfoPost infoPost,
            User author,
            String authorProfileUrl,
            List<InfoPostImage> images,
            Function<String, String> urlResolver,
            Boolean isAuthor,
            Integer recruitmentCount
    ) {
        Author authorInfo = new Author(
                author.getUserId(),
                author.getName(),
                authorProfileUrl
        );

        List<Image> imageList = images.stream()
                .map(img -> new Image(urlResolver.apply(img.getImageKey()), img.getSortOrder()))
                .toList();

        return new InfoPostDetailResponse(
                infoPost.getInfoPostId(),
                infoPost.getCategory(),
                infoPost.getLinkable(),
                infoPost.getTitle(),
                infoPost.getContent(),
                imageList,
                authorInfo,
                isAuthor,
                recruitmentCount,
                infoPost.getCreatedAt(),
                infoPost.getUpdatedAt()
        );
    }

    @Schema(description = "작성자 정보")
    private record Author(
            @Schema(description = "작성자 유저 ID", example = "8") Long authorId,
            @Schema(description = "작성자 이름", example = "누구누구") String name,
            @Schema(description = "작성자 프로필 이미지 URL") String profileUrl
    ) {}

    @Schema(description = "정보글 이미지")
    private record Image(
            @Schema(description = "이미지 URL") String imageUrl,
            @Schema(description = "정렬 순서", example = "0") Integer sortOrder
    ) {}
}