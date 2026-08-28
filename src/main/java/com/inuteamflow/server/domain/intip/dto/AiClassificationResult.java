package com.inuteamflow.server.domain.intip.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiClassificationResult {
    private boolean isRelevant;
    private String category; // InfoPostCategory 후보 4개 중 하나 (문자열로 받고 서비스에서 검증)
    private String reason;
}
