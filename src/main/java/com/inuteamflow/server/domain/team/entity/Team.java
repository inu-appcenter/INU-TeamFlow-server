package com.inuteamflow.server.domain.team.entity;

import com.inuteamflow.server.domain.team.dto.request.TeamCreateRequest;
import com.inuteamflow.server.domain.team.dto.request.TeamUpdateRequest;
import com.inuteamflow.server.global.BaseEntity;
import com.inuteamflow.server.global.enums.Category;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "team")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Long teamId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    private Category category;

    private String link;

    private String sns;

    @Column(name = "image_key")
    private String imageKey;

    @Builder
    private Team(String name, String description, Category category, String link, String sns, String imageKey) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.link = link;
        this.sns = sns;
        this.imageKey = imageKey;
    }

    public static Team create(TeamCreateRequest request) {
        return Team.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .link(request.getLink())
                .sns(request.getSns())
                .imageKey(request.getImageKey())
                .build();
    }

    public void update(TeamUpdateRequest request) {
        this.name = request.getName();
        this.description = request.getDescription();
        this.category = request.getCategory();
        this.link = request.getLink();
        this.sns = request.getSns();
        this.imageKey = request.getImageKey();
    }
}
