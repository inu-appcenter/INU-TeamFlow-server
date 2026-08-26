package com.inuteamflow.server.domain.inquiry.dto.request;

import com.inuteamflow.server.domain.inquiry.enums.InquiryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InquiryRequest {

    @NotNull
    private InquiryType type;

    @NotBlank
    @Size(max = 1000)
    private String detail;

}
