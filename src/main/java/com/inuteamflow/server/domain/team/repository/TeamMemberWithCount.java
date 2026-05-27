package com.inuteamflow.server.domain.team.repository;

import com.inuteamflow.server.domain.team.entity.TeamMember;

public interface TeamMemberWithCount {
    TeamMember getTeamMember();
    Long getMemberCount();
}
