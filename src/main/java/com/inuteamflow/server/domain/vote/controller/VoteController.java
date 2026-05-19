package com.inuteamflow.server.domain.vote.controller;

import com.inuteamflow.server.domain.event.dto.response.EventDetailResponse;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.domain.vote.dto.request.EventVoteCreateRequest;
import com.inuteamflow.server.domain.vote.dto.request.EventVoteTimeSelectRequest;
import com.inuteamflow.server.domain.vote.dto.request.EventVoteTimeSlotSelectRequest;
import com.inuteamflow.server.domain.vote.dto.response.EventVoteResponse;
import com.inuteamflow.server.domain.vote.dto.response.EventVoteTimeSlotResponse;
import com.inuteamflow.server.domain.vote.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class VoteController {

    private final VoteService voteService;

    @GetMapping("/teams/{teamId}/votes")
    public ResponseEntity<List<EventVoteResponse>> getVoteList(
            @PathVariable("teamId") Long teamId
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(voteService.getVoteList(teamId));
    }

    @PostMapping("/teams/{teamId}/votes")
    public ResponseEntity<EventVoteResponse> createVote(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable("teamId") Long teamId,
            @Valid @RequestBody EventVoteCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(voteService.createVote(userDetails, teamId, request));
    }

    @GetMapping("/votes/{voteId}")
    public ResponseEntity<EventVoteResponse> getVote(
            @PathVariable("voteId") Long voteId
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(voteService.getVote(voteId));
    }

    @GetMapping("/votes/{voteId}/slots")
    public ResponseEntity<List<EventVoteTimeSlotResponse>> getTimeSlot(
            @PathVariable("voteId") Long voteId
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(voteService.getTimeSlot(voteId));
    }

    @PutMapping("/votes/{voteId}/slots")
    public ResponseEntity<List<EventVoteTimeSlotResponse>> selectTimeSlot(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable("voteId") Long voteId,
            @Valid @RequestBody EventVoteTimeSlotSelectRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(voteService.selectTimeSlot(userDetails, voteId, request));
    }

    @PostMapping("/votes/{voteId}/result")
    public ResponseEntity<EventDetailResponse> createVoteResult(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable("voteId") Long voteId,
            @Valid @RequestBody EventVoteTimeSelectRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(voteService.createVoteResult(userDetails, voteId, request));
    }

}
