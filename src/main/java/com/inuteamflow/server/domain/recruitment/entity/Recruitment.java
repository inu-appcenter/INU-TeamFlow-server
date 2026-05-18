package com.inuteamflow.server.domain.recruitment.entity;

import com.inuteamflow.server.domain.recruitment.enums.RecruitmentCategory;
import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.global.BaseEntity;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "recruitment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recruitment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recruitment_id")
    private Long recruitmentId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    private RecruitmentCategory category;

    @Column(nullable = false, name = "is_opened")
    private Boolean isOpened;

    @Column(nullable = false, name = "target_member_count")
    private Integer targetMemberCount;

    @Column(nullable = false, name = "current_member_count")
    private Integer currentMemberCount;

    @Column(nullable = false, name = "end_at")
    private LocalDateTime endAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private User recruiter;

    @Builder
    private Recruitment(String title, String description, RecruitmentCategory category,
                        Integer targetMemberCount, LocalDateTime endAt,
                        Team team, User recruiter) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.isOpened = true;
        this.targetMemberCount = targetMemberCount;
        this.currentMemberCount = 0;
        this.endAt = endAt;
        this.team = team;
        this.recruiter = recruiter;
    }

    public void update(String title, String description, RecruitmentCategory category,
                       Integer targetMemberCount, LocalDateTime endAt) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.targetMemberCount = targetMemberCount;
        this.endAt = endAt;
    }

    public void increaseCurrentMemberCount() {
        if (this.currentMemberCount >= this.targetMemberCount) {
            throw new RestApiException(CustomErrorCode.RECRUITMENT_MEMBER_FULL);
        }
        this.currentMemberCount++;
        if (this.currentMemberCount >= this.targetMemberCount) {
            this.isOpened = false;
        }
    }

    public void close() {
        this.isOpened = false;
    }

}
