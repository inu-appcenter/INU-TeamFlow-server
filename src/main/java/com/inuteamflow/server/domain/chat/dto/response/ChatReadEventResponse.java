package com.inuteamflow.server.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "읽음 처리 실시간 브로드캐스트 DTO")
public class ChatReadEventResponse {

    @Schema(description = "채팅방 ID", example = "3")
    private Long chatRoomId;

    @Schema(description = "읽음 처리한 유저 ID", example = "8")
    private Long userId;

    @Schema(description = "그 유저가 새로 읽은 마지막 메시지 ID", example = "142")
    private Long lastReadMessageId;

    public static ChatReadEventResponse of(Long chatRoomId, Long userId, Long lastReadMessageId) {
        return new ChatReadEventResponse(chatRoomId, userId, lastReadMessageId);
    }
}
