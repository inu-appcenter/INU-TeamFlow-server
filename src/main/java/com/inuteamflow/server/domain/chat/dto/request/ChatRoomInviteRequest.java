package com.inuteamflow.server.domain.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "채팅방 멤버 초대 요청 dto")
public class ChatRoomInviteRequest {

    @NotEmpty
    @Schema(description = "초대할 팀 멤버 유저 ID 목록", example = "[9, 15]")
    private List<Long> memberIds;
}