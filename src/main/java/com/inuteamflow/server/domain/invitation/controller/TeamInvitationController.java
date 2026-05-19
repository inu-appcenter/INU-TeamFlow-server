package com.inuteamflow.server.domain.invitation.controller;

import com.inuteamflow.server.domain.invitation.dto.request.TeamInvitationCreateRequest;
import com.inuteamflow.server.domain.invitation.dto.request.TeamInvitationStatusUpdateRequest;
import com.inuteamflow.server.domain.invitation.dto.response.TeamInvitationResponse;
import com.inuteamflow.server.domain.invitation.enums.InvitationDirection;
import com.inuteamflow.server.domain.invitation.service.TeamInvitationService;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TeamInvitationController {

    private final TeamInvitationService teamInvitationService;

    @GetMapping("/teams/invitations")
    public ApiResponse<Page<TeamInvitationResponse>> getInvitations(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam InvitationDirection direction,
            Pageable pageable
    ) {
        User user = userDetails.getUser();
        return ApiResponse.ok(teamInvitationService.getInvitations(user, direction, pageable));
    }

    @PostMapping("/teams/{teamId}/invitations")
    public ApiResponse<TeamInvitationResponse> invite(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long teamId,
            @Valid @RequestBody TeamInvitationCreateRequest request
    ) {
        User user = userDetails.getUser();
        return ApiResponse.ok(teamInvitationService.invite(user, teamId, request));
    }

    @PutMapping("/teams/invitations/{invitationId}/status")
    public ApiResponse<TeamInvitationResponse> updateStatus(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long invitationId,
            @Valid @RequestBody TeamInvitationStatusUpdateRequest request
    ) {
        User user = userDetails.getUser();
        return ApiResponse.ok(teamInvitationService.updateStatus(user, invitationId, request));
    }
}
