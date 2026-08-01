package com.inuteamflow.server.domain.chat.dto.response;

import com.inuteamflow.server.domain.team.enums.TeamRole;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.enums.Department;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "채팅방 멤버 / 초대 후보 응답 dto")
public class ChatRoomMemberResponse {

    @Schema(description = "유저 ID", example = "7")
    private Long userId;

    @Schema(description = "로그인 아이디", example = "userB")
    private String username;

    @Schema(description = "실제 이름", example = "김철수")
    private String userNickname;

    @Schema(description = "학과")
    private Department department;

    @Schema(description = "팀 내 권한 (1:1 채팅방이면 null)")
    private TeamRole teamRole;

    @Schema(description = "프로필 이미지 URL")
    private String profileImageUrl;

    public static ChatRoomMemberResponse create(User user, TeamRole teamRole, String profileImageUrl) {
        return new ChatRoomMemberResponse(
                user.getUserId(), user.getUsername(), user.getName(), user.getDepartment(), teamRole, profileImageUrl);
    }
}
