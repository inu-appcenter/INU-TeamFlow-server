package com.inuteamflow.server.domain.teamNotice.controller;

import com.inuteamflow.server.domain.teamNotice.dto.req.TeamNoticeCreateRequest;
import com.inuteamflow.server.domain.teamNotice.dto.req.TeamNoticeUpdateRequest;
import com.inuteamflow.server.domain.teamNotice.dto.res.TeamNoticeDetailResponse;
import com.inuteamflow.server.domain.teamNotice.dto.res.TeamNoticeSummaryResponse;
import com.inuteamflow.server.domain.teamNotice.service.TeamNoticeService;
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
public class TeamNoticeController implements TeamNoticeControllerDocument {

    private final TeamNoticeService teamNoticeService;

    @GetMapping("/teams/{teamId}/notices")
    public ResponseEntity<Page<TeamNoticeSummaryResponse>> getTeamNotices(
            @PathVariable Long teamId,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(teamNoticeService.getTeamNotices(teamId, keyword, userDetails.getUser(), pageable));
    }

    @GetMapping("/teams/{teamId}/notices/{noticeId}")
    public ResponseEntity<TeamNoticeDetailResponse> getTeamNotice(
            @PathVariable Long teamId,
            @PathVariable Long noticeId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(teamNoticeService.getTeamNotice(teamId, noticeId, userDetails.getUser()));
    }

    @PostMapping("/teams/{teamId}/notices")
    public ResponseEntity<TeamNoticeDetailResponse> createTeamNotice(
            @PathVariable Long teamId,
            @Valid @RequestBody TeamNoticeCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamNoticeService.createTeamNotice(teamId, userDetails.getUser(), request));
    }

    @PutMapping("/teams/{teamId}/notices/{noticeId}")
    public ResponseEntity<TeamNoticeDetailResponse> updateTeamNotice(
            @PathVariable Long teamId,
            @PathVariable Long noticeId,
            @Valid @RequestBody TeamNoticeUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(teamNoticeService.updateTeamNotice(teamId, noticeId, userDetails.getUser(), request));
    }

    @DeleteMapping("/teams/{teamId}/notices/{noticeId}")
    public ResponseEntity<Void> deleteTeamNotice(
            @PathVariable Long teamId,
            @PathVariable Long noticeId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        teamNoticeService.deleteTeamNotice(teamId, noticeId, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/team-notices/me")
    public ResponseEntity<Page<TeamNoticeSummaryResponse>> getMyTeamNotices(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(teamNoticeService.getMyTeamNotices(userDetails.getUser(), pageable));
    }

    @GetMapping("/notices/me")
    public ResponseEntity<Page<TeamNoticeSummaryResponse>> getMyNotices(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(teamNoticeService.getMyNotices(userDetails.getUser(), pageable));
    }
}
