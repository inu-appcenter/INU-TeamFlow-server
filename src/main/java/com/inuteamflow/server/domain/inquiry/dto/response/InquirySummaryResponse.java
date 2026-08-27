package com.inuteamflow.server.domain.inquiry.dto.response;

import com.inuteamflow.server.domain.inquiry.entity.Inquiry;
import com.inuteamflow.server.domain.inquiry.enums.InquiryStatus;
import com.inuteamflow.server.domain.inquiry.enums.InquiryType;
import com.inuteamflow.server.global.dto.StatusSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "문의 목록 응답 DTO")
public class InquirySummaryResponse {

    @Schema(description = "문의 집계")
    private StatusSummary summary;

    @Schema(description = "문의 목록 (페이지네이션)")
    private Page<InquirySummaryItem> inquiries;

    public static InquirySummaryResponse of(StatusSummary summary, Page<InquirySummaryItem> inquiries) {
        return new InquirySummaryResponse(summary, inquiries);
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Schema(description = "문의 목록 항목 DTO")
    public static class InquirySummaryItem {

        @Schema(description = "문의 ID", example = "5")
        private Long inquiryId;

        @Schema(description = "문의 유형", example = "ACCOUNT")
        private InquiryType type;

        @Schema(description = "문의 상세 내용")
        private String detail;

        @Schema(description = "문의자 이름", example = "박OO")
        private String authorName;

        @Schema(description = "답변 상태", example = "PENDING")
        private InquiryStatus status;

        @Schema(description = "문의 접수 시각")
        private LocalDateTime createdAt;

        public static InquirySummaryItem from(Inquiry inquiry) {
            return new InquirySummaryItem(
                    inquiry.getInquiryId(),
                    inquiry.getType(),
                    inquiry.getDetail(),
                    inquiry.getInquirerName(),
                    inquiry.getStatus(),
                    inquiry.getCreatedAt());
        }
    }
}
