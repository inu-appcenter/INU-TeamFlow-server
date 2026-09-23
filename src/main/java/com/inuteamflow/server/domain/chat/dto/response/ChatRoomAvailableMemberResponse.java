package com.inuteamflow.server.domain.chat.dto.response;

import com.inuteamflow.server.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "그룹 채팅방 초대 가능한 팀원 응답 dto")
public class ChatRoomAvailableMemberResponse {

    @Schema(description = "유저 ID", example = "7")
    private Long userId;

    @Schema(description = "실제 이름", example = "홍길동")
    private String name;

    @Schema(description = "학번", example = "202012345")
    private String studentNumber;

    public static ChatRoomAvailableMemberResponse from(User user) {
        return new ChatRoomAvailableMemberResponse(user.getUserId(), user.getName(), user.getStudentNumber());
    }
}
