package com.inuteamflow.server.domain.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "1:1 채팅방 생성 요청 dto")
public class DirectChatRoomCreateRequest {

    @NotNull
    @Schema(description = "대화 상대 유저 ID", example = "5")
    private Long targetUserId;
}
