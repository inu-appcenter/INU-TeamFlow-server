package com.inuteamflow.server.domain.teamNotice.dto.res;

import com.inuteamflow.server.domain.team.enums.TeamRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공지 작성자 정보")
public record TeamNoticeAuthorResponse(
        @Schema(description = "작성자 유저 ID", example = "1") Long userId,

        @Schema(description = "작성자 팀 역할", example = "LEADER")
        TeamRole teamRole,

        @Schema(description = "작성자 이름", example = "손동민") String name,
        @Schema(description = "작성자 프로필 이미지 URL") String profileUrl) {

    /** 팀을 떠난(탈퇴·강퇴) 작성자를 표시할 때 사용하는 이름. */
    public static final String WITHDRAWN_AUTHOR_NAME = "(탈퇴한 사용자)";
}
