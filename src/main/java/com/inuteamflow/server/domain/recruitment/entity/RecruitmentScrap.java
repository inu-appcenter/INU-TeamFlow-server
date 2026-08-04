package com.inuteamflow.server.domain.recruitment.entity;

import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.global.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "recruitment_scrap", uniqueConstraints = @UniqueConstraint(columnNames = {"recruitment_id", "user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentScrap extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recruitment_scrap_id")
    private Long recruitmentScrapId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruitment_id", nullable = false)
    private Recruitment recruitment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    private RecruitmentScrap(Recruitment recruitment, User user) {
        this.recruitment = recruitment;
        this.user = user;
    }

    public static RecruitmentScrap create(Recruitment recruitment, User user) {
        return RecruitmentScrap.builder().recruitment(recruitment).user(user).build();
    }
}
