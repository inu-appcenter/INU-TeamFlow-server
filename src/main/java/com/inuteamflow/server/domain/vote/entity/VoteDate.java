package com.inuteamflow.server.domain.vote.entity;

import com.inuteamflow.server.global.BaseTimeEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "vote_date", uniqueConstraints = @UniqueConstraint(columnNames = {"vote_id", "date"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoteDate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vote_date_id")
    private Long voteDateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_id")
    private Vote vote;

    @Column(name = "date")
    private LocalDate date;

    @Builder
    private VoteDate(Vote vote, LocalDate date) {
        this.vote = vote;
        this.date = date;
    }

    public static VoteDate create(Vote vote, LocalDate date) {
        return VoteDate.builder().vote(vote).date(date).build();
    }
}
