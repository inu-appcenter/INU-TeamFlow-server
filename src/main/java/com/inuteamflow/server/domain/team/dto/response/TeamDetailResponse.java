package com.inuteamflow.server.domain.team.dto.response;

import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.domain.team.enums.TeamRole;
import com.inuteamflow.server.global.enums.Category;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TeamDetailResponse {

    private Long teamId;

    private String name;

    private Category category;

    private String description;

    private Integer memberCount;

    private TeamRole role;

    private String link;

    private String sns;

    private String imageUrl;

    private LocalDateTime joinedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static TeamDetailResponse create(Team team, TeamMember teamMember, String imageUrl, int memberCount) {
        return new TeamDetailResponse(
                team.getTeamId(),
                team.getName(),
                team.getCategory(),
                team.getDescription(),
                memberCount,
                teamMember.getTeamRole(),
                team.getLink(),
                team.getSns(),
                imageUrl,
                teamMember.getJoinedAt(),
                team.getCreatedAt(),
                team.getUpdatedAt()
        );
    }
}
