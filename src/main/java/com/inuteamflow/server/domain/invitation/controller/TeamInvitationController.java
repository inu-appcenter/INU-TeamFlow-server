package com.inuteamflow.server.domain.invitation.controller;

import com.inuteamflow.server.domain.invitation.dto.request.TeamInvitationCreateRequest;
import com.inuteamflow.server.domain.invitation.dto.request.TeamInvitationStatusUpdateRequest;
import com.inuteamflow.server.domain.invitation.dto.response.TeamInvitationResponse;
import com.inuteamflow.server.domain.invitation.enums.InvitationDirection;
import com.inuteamflow.server.domain.invitation.service.TeamInvitationService;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TeamInvitationController implements TeamInvitationControllerDocument {

    private final TeamInvitationService teamInvitationService;

    @GetMapping("/teams/invitations")
    public ResponseEntity<Page<TeamInvitationResponse>> getInvitations(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam InvitationDirection direction,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        User user = userDetails.getUser();
        return ResponseEntity.status(HttpStatus.OK)
                .body(teamInvitationService.getInvitations(user, direction, pageable));
    }

    @PostMapping("/teams/{teamId}/invitations")
    public ResponseEntity<TeamInvitationResponse> invite(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long teamId,
            @Valid @RequestBody TeamInvitationCreateRequest request
    ) {
        User user = userDetails.getUser();
        return ResponseEntity.status(HttpStatus.OK)
                .body(teamInvitationService.invite(user, teamId, request));
    }

    @PutMapping("/teams/invitations/{invitationId}/status")
    public ResponseEntity<TeamInvitationResponse> updateStatus(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long invitationId,
            @Valid @RequestBody TeamInvitationStatusUpdateRequest request
    ) {
        User user = userDetails.getUser();
        return ResponseEntity.status(HttpStatus.OK)
                .body(teamInvitationService.updateStatus(user, invitationId, request));
    }

    @PutMapping("/teams/{teamId}/invitations/{invitationId}/cancel")
    public ResponseEntity<TeamInvitationResponse> cancelInvitation(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long teamId,
            @PathVariable Long invitationId
    ) {
        User user = userDetails.getUser();
        return ResponseEntity.status(HttpStatus.OK)
                .body(teamInvitationService.cancelInvitation(user, teamId, invitationId));
    }
}
