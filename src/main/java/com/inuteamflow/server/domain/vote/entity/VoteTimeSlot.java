package com.inuteamflow.server.domain.vote.entity;

import com.inuteamflow.server.global.BaseTimeEntity;
import jakarta.persistence.*;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "vote_time_slot",
        uniqueConstraints = @UniqueConstraint(columnNames = {"vote_date_id", "slot_start_at", "slot_end_at"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoteTimeSlot extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vote_time_slot_id")
    private Long voteTimeSlotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_date_id")
    private VoteDate voteDate;

    @Column(name = "slot_start_at")
    private LocalTime slotStartAt;

    @Column(name = "slot_end_at")
    private LocalTime slotEndAt;

    @Builder
    private VoteTimeSlot(VoteDate voteDate, LocalTime slotStartAt, LocalTime slotEndAt) {
        this.voteDate = voteDate;
        this.slotStartAt = slotStartAt;
        this.slotEndAt = slotEndAt;
    }

    public static VoteTimeSlot create(VoteDate voteDate, LocalTime slotStartAt, LocalTime slotEndAt) {
        return VoteTimeSlot.builder()
                .voteDate(voteDate)
                .slotStartAt(slotStartAt)
                .slotEndAt(slotEndAt)
                .build();
    }
}
