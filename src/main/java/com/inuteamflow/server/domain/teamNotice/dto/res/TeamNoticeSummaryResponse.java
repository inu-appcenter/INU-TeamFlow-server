package com.inuteamflow.server.domain.teamNotice.dto.res;

import com.inuteamflow.server.domain.team.enums.TeamRole;
import com.inuteamflow.server.domain.teamNotice.entity.TeamNotice;
import com.inuteamflow.server.global.enums.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "팀 공지 요약 응답 DTO")
public class TeamNoticeSummaryResponse {

    @Schema(description = "공지 ID", example = "1")
    private Long noticeId;

    @Schema(description = "팀 ID", example = "1")
    private Long teamId;

    @Schema(description = "팀 이름", example = "팀플로우")
    private String teamName;

    @Schema(description = "팀 카테고리", example = "PROJECT")
    private Category teamCategory;

    @Schema(description = "공지 제목", example = "5월 정기 회의 안내")
    private String title;

    @Schema(description = "상단 고정 여부", example = "true")
    private Boolean isPinned;

    @Schema(description = "읽음 여부", example = "false")
    private Boolean isRead;

    @Schema(description = "작성자 이름", example = "손동민")
    private String authorName;

    @Schema(description = "작성자 팀 역할", example = "LEADER")
    private TeamRole teamRole;

    @Schema(description = "작성일시", example = "2026-06-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2026-06-02T15:30:00")
    private LocalDateTime updatedAt;

    public static TeamNoticeSummaryResponse of(TeamNotice notice, boolean isRead, TeamNoticeAuthorResponse author) {
        return new TeamNoticeSummaryResponse(
                notice.getTeamNoticeId(),
                notice.getTeam().getTeamId(),
                notice.getTeam().getName(),
                notice.getTeam().getCategory(),
                notice.getTitle(),
                notice.getIsPinned(),
                isRead,
                author.name(),
                author.teamRole(),
                notice.getCreatedAt(),
                notice.getUpdatedAt());
    }
}
