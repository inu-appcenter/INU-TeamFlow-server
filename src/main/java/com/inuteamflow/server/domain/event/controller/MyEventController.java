package com.inuteamflow.server.domain.event.controller;

import com.inuteamflow.server.domain.event.dto.request.MyEventCreateRequest;
import com.inuteamflow.server.domain.event.dto.request.MyEventUpdateRequest;
import com.inuteamflow.server.domain.event.dto.response.EventDetailResponse;
import com.inuteamflow.server.domain.event.dto.response.EventListResponse;
import com.inuteamflow.server.domain.event.enums.RecurrenceEditScope;
import com.inuteamflow.server.domain.event.service.MyEventService;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events")
public class MyEventController implements MyEventControllerDocument {

    private final MyEventService myEventService;

    @GetMapping
    public ResponseEntity<List<EventListResponse>> getMyEventList(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(myEventService.getMyEventList(userDetails.getUser(), year, month));
    }

    @PostMapping
    public ResponseEntity<EventDetailResponse> createMyEvent(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @Valid @RequestBody MyEventCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(myEventService.createMyEvent(userDetails.getUser(), request));
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<EventDetailResponse> updateMyEvent(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable("eventId") Long eventId,
            @Valid @RequestBody MyEventUpdateRequest request) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(myEventService.updateMyEvent(userDetails.getUser(), eventId, request));
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteMyEvent(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable("eventId") Long eventId,
            @RequestParam(name = "scope", required = false) RecurrenceEditScope recurrenceEditScope,
            @RequestParam(name = "occurrence", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime occurrenceAt) {
        myEventService.deleteMyEvent(userDetails.getUser(), eventId, recurrenceEditScope, occurrenceAt);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
