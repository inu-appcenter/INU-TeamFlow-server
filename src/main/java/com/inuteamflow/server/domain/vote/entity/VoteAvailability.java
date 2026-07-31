package com.inuteamflow.server.domain.vote.entity;

import com.inuteamflow.server.global.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "vote_availability",
        uniqueConstraints = @UniqueConstraint(columnNames = {"vote_participant_id", "vote_time_slot_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoteAvailability extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vote_availability_id")
    private Long voteAvailabilityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_participant_id")
    private VoteParticipant voteParticipant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_time_slot_id")
    private VoteTimeSlot voteTimeSlot;

    @Builder
    private VoteAvailability(VoteParticipant voteParticipant, VoteTimeSlot voteTimeSlot) {
        this.voteParticipant = voteParticipant;
        this.voteTimeSlot = voteTimeSlot;
    }

    public static VoteAvailability create(VoteParticipant voteParticipant, VoteTimeSlot voteTimeSlot) {
        return VoteAvailability.builder()
                .voteParticipant(voteParticipant)
                .voteTimeSlot(voteTimeSlot)
                .build();
    }
}
