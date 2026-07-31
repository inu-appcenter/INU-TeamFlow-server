package com.inuteamflow.server.domain.chat.dto.response;

import com.inuteamflow.server.domain.chat.enums.ChatRoomType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "채팅방 목록 요약 응답 DTO")
public class ChatRoomSummaryResponse {

    @Schema(description = "채팅방 ID", example = "3")
    private Long chatRoomId;

    @Schema(description = "팀 ID (TEAM 타입일 때만 값 존재, 팀 상세페이지 필터링용)", example = "12")
    private Long teamId;

    @Schema(description = "채팅방 타입", example = "TEAM")
    private ChatRoomType chatRoomType;

    @Schema(description = "방 이름 (TEAM이면 팀 이름, DIRECT면 상대방 이름)", example = "팀플로우")
    private String roomName;

    @Schema(description = "방 이미지 URL (팀 채팅방의 경우 리더가 커스텀 설정한 경우에만 존재)")
    private String imageUrl;

    @Schema(description = "기본 이미지용 멤버 프로필 URL 목록 (imageUrl 이 null 일 때 프론트에서 콜라주로 렌더링, 최대 4명)")
    private List<String> memberProfileUrls;

    @Schema(description = "마지막 메시지 미리보기", example = "회의 몇시인가여?")
    private String lastMessage;

    @Schema(description = "마지막 메시지 전송 시각", example = "2026-07-13T14:02:00")
    private LocalDateTime lastMessageAt;

    @Schema(description = "안읽은 메시지 수", example = "3")
    private int unreadCount;

    public static ChatRoomSummaryResponse create(
            Long chatRoomId,
            Long teamId,
            ChatRoomType chatRoomType,
            String roomName,
            String imageUrl,
            List<String> memberProfileUrls,
            String lastMessage,
            LocalDateTime lastMessageAt,
            int unreadCount) {
        return new ChatRoomSummaryResponse(
                chatRoomId,
                teamId,
                chatRoomType,
                roomName,
                imageUrl,
                memberProfileUrls,
                lastMessage,
                lastMessageAt,
                unreadCount);
    }
}
