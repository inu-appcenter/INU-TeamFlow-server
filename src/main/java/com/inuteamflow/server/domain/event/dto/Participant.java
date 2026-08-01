package com.inuteamflow.server.domain.event.dto;

import com.inuteamflow.server.domain.event.entity.EventParticipant;
import com.inuteamflow.server.domain.event.entity.RecurrenceExceptionParticipant;
import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.domain.team.enums.TeamRole;
import com.inuteamflow.server.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "팀 일정 참석자 DTO")
public class Participant {

    private Long userId;

    private Long teamMemberId;

    private String name;

    private TeamRole teamRole;

    public static Participant create(TeamMember teamMember) {
        return new Participant(
                teamMember.getUser().getUserId(),
                teamMember.getTeamMemberId(),
                teamMember.getUser().getName(),
                teamMember.getTeamRole());
    }

    public static Participant create(EventParticipant eventParticipant) {
        return create(eventParticipant.getTeamMember());
    }

    public static Participant create(RecurrenceExceptionParticipant recurrenceExceptionParticipant) {
        return create(recurrenceExceptionParticipant.getTeamMember());
    }

    public static boolean isParticipant(List<Participant> participants, User user) {
        if (participants == null || user == null) {
            return false;
        }

        return participants.stream()
                .anyMatch(participant ->
                        participant != null && Objects.equals(participant.getUserId(), user.getUserId()));
    }
}
