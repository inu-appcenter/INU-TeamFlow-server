package com.inuteamflow.server.domain.invitation.dto.response;

import com.inuteamflow.server.domain.invitation.entity.TeamInvitation;
import com.inuteamflow.server.global.enums.Status;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TeamInvitationResponse {

    private Long invitationId;
    private String teamName;
    private Status status;
    private String senderName; // 초대 보낸 사람 (createdBy -> User)
    private String receiverName; // 초대 받은 사람
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    public static TeamInvitationResponse from(TeamInvitation invitation, String senderName) {
        return new TeamInvitationResponse(
                invitation.getTeamInvitationId(),
                invitation.getTeam().getName(),
                invitation.getInvitationStatus(),
                senderName,
                invitation.getReceiver().getName(),
                invitation.getCreatedAt(),
                invitation.getRespondedAt()
        );
    }
}
