package com.inuteamflow.server.domain.intip.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AiChatMessage {
    private String role;
    private String content;
}
