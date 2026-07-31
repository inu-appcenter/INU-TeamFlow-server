package com.inuteamflow.server.domain.team.dto.response;

import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.global.enums.Category;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TeamSummaryResponse {

    private Long teamId;

    private String name;

    private Category category;

    private Integer memberCount;

    private String description;

    private String imageUrl;

    public static TeamSummaryResponse create(Team team, String imageUrl, int memberCount) {
        return new TeamSummaryResponse(
                team.getTeamId(), team.getName(), team.getCategory(), memberCount, team.getDescription(), imageUrl);
    }
}
