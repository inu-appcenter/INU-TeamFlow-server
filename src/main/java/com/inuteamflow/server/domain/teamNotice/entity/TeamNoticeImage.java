package com.inuteamflow.server.domain.teamNotice.entity;

import com.inuteamflow.server.global.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "team_notice_image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamNoticeImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_notice_image_id")
    private Long teamNoticeImageId;

    @Column(name = "image_key")
    private String imageKey;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_notice_id", nullable = false)
    private TeamNotice teamNotice;

    @Builder
    private TeamNoticeImage(String imageKey, Integer sortOrder, TeamNotice teamNotice) {
        this.imageKey = imageKey;
        this.sortOrder = sortOrder;
        this.teamNotice = teamNotice;
    }

    public static TeamNoticeImage create(String imageKey, Integer sortOrder, TeamNotice teamNotice) {
        return TeamNoticeImage.builder()
                .imageKey(imageKey)
                .sortOrder(sortOrder)
                .teamNotice(teamNotice)
                .build();
    }
}
