package com.inuteamflow.server.domain.inquiry.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InquiryHandleRequest {

    @NotBlank
    @Size(max = 1000)
    private String answer;
}
