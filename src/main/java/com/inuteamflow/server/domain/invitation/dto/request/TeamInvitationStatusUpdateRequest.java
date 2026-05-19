package com.inuteamflow.server.domain.invitation.dto.request;

import com.inuteamflow.server.domain.invitation.enums.InvitationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamInvitationStatusUpdateRequest {

    @NotNull
    private InvitationStatus status;

}
