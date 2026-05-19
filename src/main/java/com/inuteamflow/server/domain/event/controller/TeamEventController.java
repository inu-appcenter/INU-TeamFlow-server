package com.inuteamflow.server.domain.event.controller;

import com.inuteamflow.server.domain.event.dto.request.TeamEventCreateRequest;
import com.inuteamflow.server.domain.event.dto.request.TeamEventUpdateRequest;
import com.inuteamflow.server.domain.event.dto.response.EventDetailResponse;
import com.inuteamflow.server.domain.event.dto.response.EventListResponse;
import com.inuteamflow.server.domain.event.enums.RecurrenceEditScope;
import com.inuteamflow.server.domain.event.service.TeamEventService;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teams/{teamId}/events")
public class TeamEventController implements TeamEventControllerDocument{

    private final TeamEventService teamEventService;

    @GetMapping
    public ResponseEntity<List<EventListResponse>> getTeamEventList(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable("teamId") Long teamId,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(teamEventService.getTeamEventList(userDetails.getUser(), teamId, year, month));
    }

    @PostMapping
    public ResponseEntity<EventDetailResponse> createEvent(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable("teamId") Long teamId,
            @Valid @RequestBody TeamEventCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(teamEventService.createTeamEvent(userDetails.getUser(), teamId, request));
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<EventDetailResponse> updateTeamEvent(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable("teamId") Long teamId,
            @PathVariable("eventId") Long eventId,
            @Valid @RequestBody TeamEventUpdateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(teamEventService.updateTeamEvent(userDetails.getUser(), teamId, eventId, request));
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteTeamEvent(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable("teamId") Long teamId,
            @PathVariable("eventId") Long eventId,
            @RequestParam(name = "scope", required = false) RecurrenceEditScope recurrenceEditScope,
            @RequestParam(name = "occurrence", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime occurrenceAt
    ) {
        teamEventService.deleteTeamEvent(userDetails.getUser(), teamId, eventId, recurrenceEditScope, occurrenceAt);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

}
