package com.inuteamflow.server.domain.infoPost.entity;

import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.global.BaseEntity;
import com.inuteamflow.server.global.enums.Category;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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
    private Category category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    @Column(name = "linkable", nullable = false)
    private Boolean linkable;

    @Builder
    private InfoPost(Category category, String title, String content, Boolean linkable) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.linkable = linkable;
    }

    public static InfoPost create(Category category, String title, String content, Boolean linkable) {
        return InfoPost.builder()
                .category(category)
                .title(title)
                .content(content)
                .linkable(linkable)
                .build();
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public boolean isAuthor(Long userId) {
        return this.getCreatedBy().equals(userId);
    }

}
