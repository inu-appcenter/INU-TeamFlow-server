package com.inuteamflow.server.domain.vote.entity;

import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.vote.dto.request.EventVoteCreateRequest;
import com.inuteamflow.server.global.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "vote")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vote_id")
    private Long voteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "is_opened")
    private Boolean isOpened;

    @Column(name = "is_all_day")
    private Boolean isAllDay;

    @Column(name = "daily_time_start")
    private LocalTime dailyTimeStart;

    @Column(name = "daily_time_end")
    private LocalTime dailyTimeEnd;

    @Column(name = "slot_unit_minute")
    private Integer slotUnitMinute;

    @Builder
    private Vote(
            Team team,
            String title,
            String description,
            Boolean isOpened,
            Boolean isAllDay,
            LocalTime dailyTimeStart,
            LocalTime dailyTimeEnd,
            Integer slotUnitMinute) {
        this.team = team;
        this.title = title;
        this.description = description;
        this.isOpened = isOpened;
        this.isAllDay = isAllDay;
        this.dailyTimeStart = dailyTimeStart;
        this.dailyTimeEnd = dailyTimeEnd;
        this.slotUnitMinute = slotUnitMinute;
    }

    public static Vote create(Team team, EventVoteCreateRequest request) {
        return Vote.builder()
                .team(team)
                .title(request.getTitle())
                .description(request.getDescription())
                .isOpened(true)
                .isAllDay(request.getIsAllDay())
                .dailyTimeStart(request.getDailyTimeStart())
                .dailyTimeEnd(request.getDailyTimeEnd())
                .slotUnitMinute(30)
                .build();
    }

    public void close() {
        this.isOpened = false;
    }
}
