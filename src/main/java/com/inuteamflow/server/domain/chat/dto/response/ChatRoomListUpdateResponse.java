package com.inuteamflow.server.domain.chat.dto.response;

import com.inuteamflow.server.domain.chat.enums.ChatRoomType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "채팅방 목록 실시간 갱신 브로드캐스트 DTO")
public class ChatRoomListUpdateResponse {

    @Schema(description = "채팅방 ID", example = "3")
    private Long roomId;

    @Schema(description = "마지막 메시지 정보 (메시지가 없으면 null)")
    private LastMessage lastMessage;

    @Schema(description = "채팅방 상태 갱신 시각")
    private LocalDateTime updatedAt;

    @Schema(description = "이 유저 기준 안읽은 메시지 수", example = "4")
    private int unreadCount;

    @Schema(description = "채팅방 유형")
    private ChatRoomType roomType;

    public static ChatRoomListUpdateResponse of(
            Long roomId, LastMessage lastMessage, LocalDateTime updatedAt, int unreadCount, ChatRoomType roomType) {
        return new ChatRoomListUpdateResponse(roomId, lastMessage, updatedAt, unreadCount, roomType);
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Schema(description = "채팅방 목록용 마지막 메시지 요약")
    public static class LastMessage {

        @Schema(description = "메시지 내용 (이미지 메시지는 안내 문구로 대체)")
        private String content;

        @Schema(description = "발신자 ID", example = "8")
        private Long senderId;

        @Schema(description = "발신자 이름", example = "홍길동")
        private String senderName;

        @Schema(description = "발신 시각")
        private LocalDateTime sentAt;

        public static LastMessage of(String content, Long senderId, String senderName, LocalDateTime sentAt) {
            return new LastMessage(content, senderId, senderName, sentAt);
        }
    }
}
