package com.inuteamflow.server.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "채팅방 최초 진입 시 메시지 조회 응답 DTO")
public class ChatMessageAnchorResponse {

    @Schema(description = "안읽음 시작 기준점 (이 메시지 다음부터 안읽음), 없으면 null", example = "142")
    private Long lastReadMessageId;

    @Schema(description = "이 응답보다 더 과거 메시지가 있는지 여부", example = "true")
    private boolean hasMoreBefore;

    @Schema(description = "읽은 것 최근 5개 + 안읽은 메시지 목록 (오래된순)")
    private List<ChatMessageResponse> messages;

    public static ChatMessageAnchorResponse of(Long lastReadMessageId, boolean hasMoreBefore, List<ChatMessageResponse> messages) {
        return new ChatMessageAnchorResponse(lastReadMessageId, hasMoreBefore, messages);
    }
}
