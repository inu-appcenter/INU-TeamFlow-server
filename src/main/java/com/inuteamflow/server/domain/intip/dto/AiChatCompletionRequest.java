package com.inuteamflow.server.domain.intip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiChatCompletionRequest {
    private String model;
    private List<AiChatMessage> messages;
    private double temperature;

    @JsonProperty("chat_template_kwargs")
    private Map<String, Object> chatTemplateKwargs;

    @JsonProperty("response_format")
    private Map<String, Object> responseFormat;
}
