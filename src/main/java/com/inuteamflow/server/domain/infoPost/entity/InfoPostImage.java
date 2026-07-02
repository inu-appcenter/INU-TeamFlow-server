package com.inuteamflow.server.domain.infoPost.entity;

import com.inuteamflow.server.global.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "info_post_image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InfoPostImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "info_post_image_id")
    private Long infoPostImageId;

    @Column(name = "image_key")
    private String imageKey;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "info_post_id", nullable = false)
    private InfoPost infoPost;

    @Builder
    private InfoPostImage(String imageKey, Integer sortOrder, InfoPost infoPost) {
        this.imageKey = imageKey;
        this.sortOrder = sortOrder;
        this.infoPost = infoPost;
    }

    public static InfoPostImage create(String imageKey, Integer sortOrder, InfoPost infoPost) {
        return InfoPostImage.builder()
                .imageKey(imageKey)
                .sortOrder(sortOrder)
                .infoPost(infoPost)
                .build();
    }
}
