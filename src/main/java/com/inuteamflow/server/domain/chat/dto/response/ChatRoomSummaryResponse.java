package com.inuteamflow.server.domain.chat.dto.response;

import com.inuteamflow.server.domain.chat.enums.ChatRoomType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "채팅방 목록 요약 응답 DTO")
public class ChatRoomSummaryResponse {

    @Schema(description = "채팅방 ID", example = "3")
    private Long chatRoomId;

    @Schema(description = "채팅방 타입", example = "TEAM")
    private ChatRoomType chatRoomType;

    @Schema(description = "방 이름 (TEAM이면 팀 이름, DIRECT면 상대방 이름)", example = "팀플로우")
    private String roomName;

    @Schema(description = "방 이미지 URL (팀 이미지 또는 상대방 프로필)")
    private String imageUrl;

    @Schema(description = "마지막 메시지 미리보기", example = "회의 몇시인가여?")
    private String lastMessage;

    @Schema(description = "마지막 메시지 전송 시각", example = "2026-07-13T14:02:00")
    private LocalDateTime lastMessageAt;

    @Schema(description = "안읽은 메시지 수", example = "3")
    private int unreadCount;

    public static ChatRoomSummaryResponse create(
            Long chatRoomId,
            ChatRoomType chatRoomType,
            String roomName,
            String imageUrl,
            String lastMessage,
            LocalDateTime lastMessageAt,
            int unreadCount
    ) {
        return new ChatRoomSummaryResponse(
                chatRoomId, chatRoomType, roomName, imageUrl, lastMessage, lastMessageAt, unreadCount
        );
    }
}
