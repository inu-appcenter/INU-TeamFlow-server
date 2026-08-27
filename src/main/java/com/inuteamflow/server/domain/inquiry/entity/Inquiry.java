package com.inuteamflow.server.domain.inquiry.entity;

import com.inuteamflow.server.domain.inquiry.enums.InquiryStatus;
import com.inuteamflow.server.domain.inquiry.enums.InquiryType;
import com.inuteamflow.server.global.BaseTimeEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "inquiry")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_id")
    private Long inquiryId;

    @Column(name = "inquirer_id", nullable = false)
    private Long inquirerId;

    @Column(name = "inquirer_name", nullable = false)
    private String inquirerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InquiryType type;

    @Column(length = 1000)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InquiryStatus status;

    @Column
    private String answer;

    @Column(name = "answerer_id")
    private Long answererId;

    @Column(name = "answerer_name")
    private String answererName;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Builder
    private Inquiry(Long inquirerId, String inquirerName, InquiryType type, String detail) {
        this.inquirerId = inquirerId;
        this.inquirerName = inquirerName;
        this.type = type;
        this.detail = detail;
        this.status = InquiryStatus.PENDING;
    }

    public static Inquiry create(Long inquirerId, String inquirerName, InquiryType type, String detail) {
        return Inquiry.builder()
                .inquirerId(inquirerId)
                .inquirerName(inquirerName)
                .type(type)
                .detail(detail)
                .build();
    }

    public boolean isOwnedBy(Long userId) {
        return this.inquirerId.equals(userId);
    }

    public void answer(String answer, Long answererId, String answererName, LocalDateTime answeredAt) {
        this.answer = answer;
        this.answererId = answererId;
        this.answererName = answererName;
        this.answeredAt = answeredAt;
        this.status = InquiryStatus.RESOLVED;
    }
}
