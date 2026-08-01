package com.inuteamflow.server.domain.vote.entity;

import com.inuteamflow.server.domain.event.entity.Event;
import com.inuteamflow.server.global.BaseTimeEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "vote_result")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoteResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vote_result_id")
    private Long voteResultId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_id")
    private Vote vote;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(name = "is_all_day")
    private Boolean isAllDay;

    @Column(name = "selected_start_at")
    private LocalDateTime selectedStartAt;

    @Column(name = "selected_end_at")
    private LocalDateTime selectedEndAt;

    @Builder
    private VoteResult(
            Vote vote, Event event, Boolean isAllDay, LocalDateTime selectedStartAt, LocalDateTime selectedEndAt) {
        this.vote = vote;
        this.event = event;
        this.isAllDay = isAllDay;
        this.selectedStartAt = selectedStartAt;
        this.selectedEndAt = selectedEndAt;
    }

    public static VoteResult create(
            Vote vote, Event event, Boolean isAllDay, LocalDateTime selectedStartAt, LocalDateTime selectedEndAt) {
        return VoteResult.builder()
                .vote(vote)
                .event(event)
                .isAllDay(isAllDay)
                .selectedStartAt(selectedStartAt)
                .selectedEndAt(selectedEndAt)
                .build();
    }
}
