package com.inuteamflow.server.domain.invitation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamInvitationCreateRequest {

    @NotBlank
    private String studentNumber;
}
