package com.inuteamflow.server.domain.team.controller;

import com.inuteamflow.server.domain.team.dto.request.TeamCreateRequest;
import com.inuteamflow.server.domain.team.dto.request.TeamMemberRoleRequest;
import com.inuteamflow.server.domain.team.dto.request.TeamUpdateRequest;
import com.inuteamflow.server.domain.team.dto.response.TeamDetailResponse;
import com.inuteamflow.server.domain.team.dto.response.TeamMemberResponse;
import com.inuteamflow.server.domain.team.dto.response.TeamSummaryResponse;
import com.inuteamflow.server.domain.team.service.TeamService;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.global.response.ApiResponse;
import com.inuteamflow.server.global.s3.PresignedUrlRequest;
import com.inuteamflow.server.global.s3.PresignedUrlResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teams")
public class TeamController {

    private final TeamService teamService;

    @GetMapping("/me")
    public ApiResponse<List<TeamSummaryResponse>> getMyTeams(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        return ApiResponse.ok(teamService.getMyTeams(userDetails));
    }

    @GetMapping("/{teamId}")
    public ApiResponse<TeamDetailResponse> getTeamDatail(
            @PathVariable Long teamId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        return ApiResponse.ok(teamService.getTeamDetails(teamId, userDetails));
    }

    @GetMapping("/{teamId}/members")
    public ApiResponse<List<TeamMemberResponse>> getTeamMembers(
            @PathVariable Long teamId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        return ApiResponse.ok(teamService.getTeamMembers(teamId, userDetails));
    }

    @PatchMapping("/{teamId}/members/{memberId}/role")
    public ApiResponse<String> updateMemberRole(
            @PathVariable Long teamId,
            @PathVariable Long memberId,
            @RequestBody TeamMemberRoleRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        teamService.updateMemberRole(userDetails, teamId, memberId, request.getTeamRole());
        return ApiResponse.ok("변경 되었습니다.");
    }


    @PostMapping
    public ApiResponse<TeamDetailResponse> createTeam(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody TeamCreateRequest request
    ) {
        return ApiResponse.ok(teamService.createTeam(userDetails, request));
    }

    @PutMapping("/{teamId}")
    public ApiResponse<TeamDetailResponse> updateTeam(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long teamId,
            @Valid @RequestBody TeamUpdateRequest request
    ) {
        return ApiResponse.ok(teamService.updateTeam(userDetails, teamId, request));
    }

    @DeleteMapping("/{teamId}")
    public ApiResponse<Void> deleteTeam(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long teamId
    ) {
        teamService.deleteTeam(userDetails, teamId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{teamId}/banner/presigned-url")
    public ApiResponse<PresignedUrlResponse> getPresignedUrl(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long teamId,
            @Valid @RequestBody PresignedUrlRequest request
    ) {
        return ApiResponse.ok(teamService.getPresignedUrl(userDetails, teamId, request));
    }
}
