package com.inuteamflow.server.domain.infoPost.entity;

import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.global.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "info_post_scrap", uniqueConstraints = @UniqueConstraint(columnNames = {"info_post_id", "user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InfoPostScrap extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "info_post_scrap_id")
    private Long infoPostScrapId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "info_post_id", nullable = false)
    private InfoPost infoPost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    private InfoPostScrap(InfoPost infoPost, User user) {
        this.infoPost = infoPost;
        this.user = user;
    }

    public static InfoPostScrap create(InfoPost infoPost, User user) {
        return InfoPostScrap.builder().infoPost(infoPost).user(user).build();
    }
}
