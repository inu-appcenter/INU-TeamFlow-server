package com.inuteamflow.server.domain.inquiry.dto.response;

import com.inuteamflow.server.domain.inquiry.entity.Inquiry;
import com.inuteamflow.server.domain.inquiry.enums.InquiryStatus;
import com.inuteamflow.server.domain.inquiry.enums.InquiryType;
import com.inuteamflow.server.global.dto.UserRef;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "문의 상세 응답 DTO (사용자/관리자 공용)")
public class InquiryDetailResponse {

    @Schema(description = "문의 ID", example = "1")
    private Long inquiryId;

    @Schema(description = "문의 유형", example = "BUG")
    private InquiryType type;

    @Schema(description = "문의 상세 내용")
    private String detail;

    @Schema(description = "답변 상태", example = "RESOLVED")
    private InquiryStatus status;

    @Schema(description = "문의자")
    private UserRef inquirer;

    @Schema(description = "답변 내용 (PENDING이면 null)")
    private String answer;

    @Schema(description = "답변자 (PENDING이면 null)")
    private UserRef answeredBy;

    @Schema(description = "답변 시각 (PENDING이면 null)")
    private LocalDateTime answeredAt;

    @Schema(description = "문의 접수 시각")
    private LocalDateTime createdAt;

    public static InquiryDetailResponse from(Inquiry inquiry) {
        boolean isAnswered = inquiry.getStatus() == InquiryStatus.RESOLVED;

        return new InquiryDetailResponse(
                inquiry.getInquiryId(),
                inquiry.getType(),
                inquiry.getDetail(),
                inquiry.getStatus(),
                UserRef.of(inquiry.getInquirerId(), inquiry.getInquirerName()),
                isAnswered ? inquiry.getAnswer() : null,
                isAnswered ? UserRef.of(inquiry.getAnswererId(), inquiry.getAnswererName()) : null,
                isAnswered ? inquiry.getAnsweredAt() : null,
                inquiry.getCreatedAt());

    }
}