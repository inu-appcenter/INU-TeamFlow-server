package com.inuteamflow.server.domain.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "그룹 채팅방 생성 요청 dto")
public class GroupChatRoomCreateRequest {

    @NotNull
    @Schema(description = "채팅방을 생성할 팀 ID", example = "12")
    private Long teamId;

    @Schema(description = "채팅방 이름 (미입력 시 참여자 이름으로 자동 표시)", example = "디자인팀 회의방")
    private String roomName;

    @Schema(description = "채팅방 이미지 키 (미입력 시 멤버 프로필 콜라주로 자동 표시)")
    private String imageKey;

    @NotEmpty
    @Schema(description = "초대할 팀 멤버 유저 ID 목록 (본인 제외, 본인은 자동 포함됨)", example = "[3, 7, 12]")
    private List<Long> memberIds;

}
