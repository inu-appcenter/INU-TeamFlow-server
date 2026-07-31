package com.inuteamflow.server.domain.team.dto.request;

import com.inuteamflow.server.domain.team.enums.TeamRole;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamMemberRoleRequest {

    @NotNull
    private TeamRole teamRole;
}
