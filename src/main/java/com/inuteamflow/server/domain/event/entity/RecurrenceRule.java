package com.inuteamflow.server.domain.event.entity;

import com.inuteamflow.server.domain.event.dto.Recurrence;
import com.inuteamflow.server.domain.event.enums.RecurrenceFrequency;
import com.inuteamflow.server.global.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "recurrence_rule")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecurrenceRule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recurrence_rule_id")
    private Long recurrenceRuleId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private Event event;

    @Column(name = "freq")
    @Enumerated(EnumType.STRING)
    private RecurrenceFrequency freq;

    @Column(name = "interval_value")
    private Integer intervalValue;

    // 별도의 테이블을 생성하여 byDay를 리스트로 관리한다.
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "recurrence_rule_by_day",
            joinColumns = @JoinColumn(name = "recurrence_rule_id")
    )
    @Column(name = "by_day")
    @Enumerated(EnumType.STRING)
    private List<DayOfWeek> byDay = new ArrayList<>();

    @Column(name = "by_month_day")
    private Integer byMonthDay;

    @Column(name = "series_start_at")
    private LocalDateTime seriesStartAt;

    @Column(name = "until_at")
    private LocalDateTime untilAt;

    @Column(name = "occurrence_count")
    private Integer occurrenceCount;

    @Builder
    private RecurrenceRule(
            Event event,
            RecurrenceFrequency freq,
            Integer intervalValue,
            List<DayOfWeek> byDay,
            Integer byMonthDay,
            LocalDateTime seriesStartAt,
            LocalDateTime untilAt,
            Integer occurrenceCount
    ) {
        this.event = event;
        this.freq = freq;
        this.intervalValue = intervalValue;
        this.byDay = byDay == null ? new ArrayList<>() : new ArrayList<>(byDay);
        this.byMonthDay = byMonthDay;
        this.seriesStartAt = seriesStartAt;
        this.untilAt = untilAt;
        this.occurrenceCount = occurrenceCount;
    }

    public static RecurrenceRule create(
            Event event,
            Recurrence recurrence,
            LocalDateTime seriesStartAt
    ) {
        return RecurrenceRule.builder()
                .event(event)
                .freq(recurrence.getFreq())
                .intervalValue(recurrence.getIntervalValue())
                .byDay(recurrence.getByDay())
                .byMonthDay(recurrence.getByMonthDay())
                .seriesStartAt(seriesStartAt)
                .untilAt(recurrence.getUntilAt())
                .occurrenceCount(recurrence.getOccurrenceCount())
                .build();
    }

    public Long getEventId() {
        return event.getEventId();
    }

    public void update(
            Recurrence recurrence,
            LocalDateTime seriesStartAt
    ) {
        this.freq = recurrence.getFreq();
        this.intervalValue = recurrence.getIntervalValue();
        this.byDay = recurrence.getByDay() == null
                ? new ArrayList<>()
                : new ArrayList<>(recurrence.getByDay());
        this.byMonthDay = recurrence.getByMonthDay();
        this.seriesStartAt = seriesStartAt;
        this.untilAt = recurrence.getUntilAt();
        this.occurrenceCount = recurrence.getOccurrenceCount();
    }

    public void finishBefore(LocalDateTime occurrenceAt) {
        // DB timestamp 컬럼은 초 미만 정밀도가 낮아 minusNanos(1)로 만든 경계값이 저장 후 반올림되어 occurrenceAt과 같아진다.
        // 초 단위로 차감해 이 문제를 피한다.
        // THIS_AND_FOLLOWING 으로 수정 및 삭제 시 분기의 기준이 되는 일정도 함께 처리하는 효과를 낸다.
        this.untilAt = occurrenceAt.minusSeconds(1);
        this.occurrenceCount = null;
    }
}
