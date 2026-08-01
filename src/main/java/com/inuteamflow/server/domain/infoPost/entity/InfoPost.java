package com.inuteamflow.server.domain.infoPost.entity;

import com.inuteamflow.server.domain.infoPost.enums.InfoPostCategory;
import com.inuteamflow.server.domain.infoPost.enums.InfoPostType;
import com.inuteamflow.server.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "info_post")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InfoPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "info_post_id")
    private Long infoPostId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InfoPostCategory category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    @Builder
    private InfoPost(InfoPostCategory category, String title, String content) {
        this.category = category;
        this.title = title;
        this.content = content;
    }

    public static InfoPost create(InfoPostCategory category, String title, String content) {
        return InfoPost.builder()
                .category(category)
                .title(title)
                .content(content)
                .build();
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public boolean isAuthor(Long userId) {
        return this.getCreatedBy().equals(userId);
    }

    // 모집글 연결 가능 여부 — DB에 저장하지 않고 카테고리 대분류로 그때그때 계산
    public boolean isLinkable() {
        return this.category.getType() == InfoPostType.NOTICE;
    }
}
