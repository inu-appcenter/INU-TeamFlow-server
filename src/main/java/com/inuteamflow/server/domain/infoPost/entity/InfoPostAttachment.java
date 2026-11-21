package com.inuteamflow.server.domain.infoPost.entity;

import com.inuteamflow.server.global.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * INTIP에서 수집한 공지의 이미지 외 첨부파일(pdf, hwp 등).
 *
 * <p>파일은 저장하지 않고 학교 서버의 다운로드 URL만 보관한다.</p>
 */
@Getter
@Entity
@Table(name = "info_post_attachment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InfoPostAttachment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "info_post_attachment_id")
    private Long infoPostAttachmentId;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "file_type", length = 20)
    private String fileType;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "info_post_id", nullable = false)
    private InfoPost infoPost;

    @Builder
    private InfoPostAttachment(String name, String url, String fileType, Integer sortOrder, InfoPost infoPost) {
        this.name = name;
        this.url = url;
        this.fileType = fileType;
        this.sortOrder = sortOrder;
        this.infoPost = infoPost;
    }

    public static InfoPostAttachment create(
            String name, String url, String fileType, Integer sortOrder, InfoPost infoPost) {
        return InfoPostAttachment.builder()
                .name(name)
                .url(url)
                .fileType(fileType)
                .sortOrder(sortOrder)
                .infoPost(infoPost)
                .build();
    }
}
