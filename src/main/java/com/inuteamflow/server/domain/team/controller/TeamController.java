package com.inuteamflow.server.domain.team.controller;

import com.inuteamflow.server.domain.team.dto.request.TeamCreateRequest;
import com.inuteamflow.server.domain.team.dto.request.TeamMemberRoleRequest;
import com.inuteamflow.server.domain.team.dto.request.TeamUpdateRequest;
import com.inuteamflow.server.domain.team.dto.response.TeamDetailResponse;
import com.inuteamflow.server.domain.team.dto.response.TeamMemberResponse;
import com.inuteamflow.server.domain.team.dto.response.TeamSummaryResponse;
import com.inuteamflow.server.domain.team.service.TeamService;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teams")
public class TeamController implements TeamControllerDocument {

    private final TeamService teamService;

    @GetMapping("/me")
    public ResponseEntity<List<TeamSummaryResponse>> getMyTeams(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(teamService.getMyTeams(userDetails.getUser()));
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<TeamDetailResponse> getTeamDetail(
            @PathVariable Long teamId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(teamService.getTeamDetails(teamId, userDetails.getUser()));
    }

    @GetMapping("/{teamId}/members")
    public ResponseEntity<List<TeamMemberResponse>> getTeamMembers(
            @PathVariable Long teamId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(teamService.getTeamMembers(teamId, userDetails.getUser()));
    }

    @PatchMapping("/{teamId}/members/{memberId}/role")
    public ResponseEntity<String> updateMemberRole(
            @PathVariable Long teamId,
            @PathVariable Long memberId,
            @RequestBody TeamMemberRoleRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        teamService.updateMemberRole(userDetails.getUser(), teamId, memberId, request.getTeamRole());
        return ResponseEntity.status(HttpStatus.OK)
                .body("변경 되었습니다.");
    }


    @PostMapping
    public ResponseEntity<TeamDetailResponse> createTeam(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody TeamCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(teamService.createTeam(userDetails.getUser(), request));
    }

    @PutMapping("/{teamId}")
    public ResponseEntity<TeamDetailResponse> updateTeam(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long teamId,
            @Valid @RequestBody TeamUpdateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(teamService.updateTeam(userDetails.getUser(), teamId, request));
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> deleteTeam(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long teamId
    ) {
        teamService.deleteTeam(userDetails.getUser(), teamId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(null);
    }

    @DeleteMapping("/{teamId}/members/me")
    public ResponseEntity<Void> leaveTeam(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long teamId
    ) {
        teamService.leaveTeam(userDetails.getUser(), teamId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/{teamId}/members/{memberId}")
    public ResponseEntity<Void> kickMember(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long teamId,
            @PathVariable Long memberId
    ) {
        teamService.kickMember(userDetails.getUser(), teamId, memberId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
