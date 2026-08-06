package com.inuteamflow.server.domain.chat.dto.response;

import com.inuteamflow.server.domain.chat.entity.ChatMessage;
import com.inuteamflow.server.domain.chat.enums.ChatMessageType;
import com.inuteamflow.server.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "채팅 메시지 응답 DTO")
public class ChatMessageResponse {

    @Schema(description = "메시지 ID", example = "142")
    private Long chatMessageId;

    @Schema(description = "채팅방 ID", example = "3")
    private Long chatRoomId;

    @Schema(description = "발신자 ID", example = "8")
    private Long senderId;

    @Schema(description = "발신자 이름", example = "김희영")
    private String senderName;

    @Schema(description = "발신자 프로필 이미지 URL")
    private String senderProfileUrl;

    @Schema(description = "메시지 타입", example = "TEXT")
    private ChatMessageType messageType;

    @Schema(description = "텍스트 내용", example = "다람쥐입니다")
    private String content;

    @Schema(description = "이미지 URL (imageKey → CloudFront 변환)")
    private String imageUrl;

    @Schema(description = "전송 시각", example = "2026-07-13T14:02:00")
    private LocalDateTime createdAt;

    @Schema(description = "이 메시지를 읽은 인원 수 (발신자 본인 제외, 누가 읽었는지는 노출 안 함)", example = "2")
    private int readCount;

    @Schema(description = "이 메시지를 볼 수 있는 인원 수 (발신자 본인 제외, 합류 시점 기준으로 이 메시지 이후 합류한 멤버는 제외)", example = "3")
    private int visibleMemberCount;

    public static ChatMessageResponse of(
            ChatMessage message,
            User sender,
            Function<String, String> imageUrlResolver,
            int readCount,
            int visibleMemberCount) {
        boolean isImage = message.getMessageType() == ChatMessageType.IMAGE;

        return new ChatMessageResponse(
                message.getChatMessageId(),
                message.getChatRoom().getChatRoomId(),
                sender.getUserId(),
                sender.getName(),
                imageUrlResolver.apply(sender.getImageKey()),
                message.getMessageType(),
                message.getContent(),
                isImage ? imageUrlResolver.apply(message.getImageKey()) : null,
                message.getCreatedAt(),
                readCount,
                visibleMemberCount);
    }
}
