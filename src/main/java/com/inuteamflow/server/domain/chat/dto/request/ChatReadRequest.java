package com.inuteamflow.server.domain.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "채팅 읽음 처리 요청 dto")
public class ChatReadRequest {

    @NotNull
    @Schema(description = "마지막으로 읽은 메시지 ID", example = "142")
    private Long lastReadMessageId;
}
