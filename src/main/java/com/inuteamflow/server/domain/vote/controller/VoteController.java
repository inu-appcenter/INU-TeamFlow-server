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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class VoteController implements VoteControllerDocument {

    private final VoteService voteService;

    @GetMapping("/teams/{teamId}/votes")
    public ResponseEntity<List<EventVoteResponse>> getVotes(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable("teamId") Long teamId) {
        return ResponseEntity.status(HttpStatus.OK).body(voteService.getVotes(userDetails.getUser(), teamId));
    }

    @PostMapping("/teams/{teamId}/votes")
    public ResponseEntity<EventVoteResponse> createVote(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable("teamId") Long teamId,
            @Valid @RequestBody EventVoteCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(voteService.createVote(userDetails.getUser(), teamId, request));
    }

    @GetMapping("/votes/{voteId}")
    public ResponseEntity<EventVoteResponse> getVote(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable("voteId") Long voteId) {
        return ResponseEntity.status(HttpStatus.OK).body(voteService.getVote(userDetails.getUser(), voteId));
    }

    @GetMapping("/votes/{voteId}/slots")
    public ResponseEntity<List<EventVoteTimeSlotResponse>> getTimeSlot(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable("voteId") Long voteId) {
        return ResponseEntity.status(HttpStatus.OK).body(voteService.getTimeSlot(userDetails.getUser(), voteId));
    }

    @PutMapping("/votes/{voteId}/slots")
    public ResponseEntity<List<EventVoteTimeSlotResponse>> selectTimeSlot(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable("voteId") Long voteId,
            @Valid @RequestBody EventVoteTimeSlotSelectRequest request) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(voteService.selectTimeSlot(userDetails.getUser(), voteId, request));
    }

    @PostMapping("/votes/{voteId}/result")
    public ResponseEntity<EventDetailResponse> createVoteResult(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable("voteId") Long voteId,
            @Valid @RequestBody EventVoteTimeSelectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(voteService.createVoteResult(userDetails.getUser(), voteId, request));
    }

    @DeleteMapping("/votes/{voteId}")
    public ResponseEntity<Void> deleteVote(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable("voteId") Long voteId) {
        voteService.deleteVote(userDetails.getUser(), voteId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/votes/me")
    public ResponseEntity<List<EventVoteResponse>> getMyVotes(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.OK).body(voteService.getMyVotes(userDetails.getUser()));
    }
}
