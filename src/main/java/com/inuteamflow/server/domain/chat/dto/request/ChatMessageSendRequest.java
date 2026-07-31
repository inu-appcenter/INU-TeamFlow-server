package com.inuteamflow.server.domain.chat.dto.request;

import com.inuteamflow.server.domain.chat.enums.ChatMessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageSendRequest {

    @NotNull
    @Schema(description = "메시지 타입", example = "TEXT")
    private ChatMessageType messageType;

    @Schema(description = "텍스트 내용 (TEXT일 때 필수)", example = "안녕하세용가리")
    private String content;

    @Schema(description = "이미지 S3 키 (IMAGE일 때 필수)", example = "chat/image/550e8...")
    private String imageKey;
}
