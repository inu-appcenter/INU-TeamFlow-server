package com.inuteamflow.server.domain.invitation.controller;

import com.inuteamflow.server.domain.invitation.dto.response.TeamInvitationResponse;
import com.inuteamflow.server.domain.invitation.enums.InvitationDirection;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class InvitationController {

    @GetMapping("/teams/invitation")
    public ApiResponse<Page<TeamInvitationResponse>> getInvitations(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam InvitationDirection direction,
            Pageable pageable
    ) {
        return 
    }
}
