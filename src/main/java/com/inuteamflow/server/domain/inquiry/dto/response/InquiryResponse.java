package com.inuteamflow.server.domain.inquiry.dto.response;

import com.inuteamflow.server.domain.inquiry.entity.Inquiry;
import com.inuteamflow.server.domain.inquiry.enums.InquiryStatus;
import com.inuteamflow.server.domain.inquiry.enums.InquiryType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "문의 응답 DTO")
public class InquiryResponse {

    @Schema(description = "문의 ID", example = "1")
    private Long inquiryId;

    @Schema(description = "문의 유형", example = "BUG")
    private InquiryType type;

    @Schema(description = "문의 상세 내용")
    private String detail;

    @Schema(description = "답변 상태", example = "PENDING")
    private InquiryStatus status;

    @Schema(description = "문의 접수 시각")
    private LocalDateTime createdAt;

    public static InquiryResponse from(Inquiry inquiry) {
        return new InquiryResponse(
                inquiry.getInquiryId(),
                inquiry.getType(),
                inquiry.getDetail(),
                inquiry.getStatus(),
                inquiry.getCreatedAt());
    }

}