package com.inuteamflow.server.domain.vote.controller;

import com.inuteamflow.server.domain.event.dto.response.EventDetailResponse;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.domain.vote.dto.request.EventVoteCreateRequest;
import com.inuteamflow.server.domain.vote.dto.request.EventVoteTimeSelectRequest;
import com.inuteamflow.server.domain.vote.dto.request.EventVoteTimeSlotSelectRequest;
import com.inuteamflow.server.domain.vote.dto.response.EventVoteResponse;
import com.inuteamflow.server.domain.vote.dto.response.EventVoteTimeSlotResponse;
import com.inuteamflow.server.global.exception.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Vote Controller", description = "투표 컨트롤러")
public interface VoteControllerDocument {

    @Operation(summary = "getVoteList", description = "팀 투표 목록 조회")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "팀 투표 목록 조회 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                array = @ArraySchema(schema = @Schema(implementation = EventVoteResponse.class)))),
        @ApiResponse(
                responseCode = "401",
                description = "인증 실패 또는 만료된 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "정지된 사용자",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "팀을 찾을 수 없음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<List<EventVoteResponse>> getVotes(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable("teamId") Long teamId);

    @Operation(summary = "createVote", description = "투표 생성")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "투표 생성 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = EventVoteResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 값",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "인증 실패 또는 만료된 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "정지된 사용자",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "팀 또는 팀 멤버를 찾을 수 없음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<EventVoteResponse> createVote(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable("teamId") Long teamId,
            @Valid @RequestBody EventVoteCreateRequest request);

    @Operation(summary = "getVote", description = "투표 상세 조회")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "투표 상세 조회 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = EventVoteResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "인증 실패 또는 만료된 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "정지된 사용자",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "투표를 찾을 수 없음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<EventVoteResponse> getVote(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable("voteId") Long voteId);

    @Operation(summary = "getTimeSlot", description = "투표 시간 슬롯 조회")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "투표 시간 슬롯 조회 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                array =
                                        @ArraySchema(
                                                schema = @Schema(implementation = EventVoteTimeSlotResponse.class)))),
        @ApiResponse(
                responseCode = "401",
                description = "인증 실패 또는 만료된 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "정지된 사용자",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "투표 또는 팀 멤버를 찾을 수 없음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<List<EventVoteTimeSlotResponse>> getTimeSlot(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable("voteId") Long voteId);

    @Operation(summary = "selectTimeSlot", description = "투표 시간 슬롯 선택")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "투표 시간 슬롯 선택 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                array =
                                        @ArraySchema(
                                                schema = @Schema(implementation = EventVoteTimeSlotResponse.class)))),
        @ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 값 또는 닫힌 투표/유효하지 않은 시간 슬롯",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "인증 실패 또는 만료된 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "정지된 사용자",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "투표, 팀 멤버, 투표 참여자 또는 시간 슬롯을 찾을 수 없음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<List<EventVoteTimeSlotResponse>> selectTimeSlot(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable("voteId") Long voteId,
            @Valid @RequestBody EventVoteTimeSlotSelectRequest request);

    @Operation(summary = "createVoteResult", description = "투표 결과 확정 및 일정 생성")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "투표 결과 확정 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = EventDetailResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 값 또는 닫힌 투표/유효하지 않은 결과 확정 시간",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "인증 실패 또는 만료된 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "정지된 사용자",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "투표 또는 팀 멤버를 찾을 수 없음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "이미 투표 결과가 확정됨",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<EventDetailResponse> createVoteResult(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable("voteId") Long voteId,
            @Valid @RequestBody EventVoteTimeSelectRequest request);

    @Operation(summary = "deleteVote", description = "투표 포함, 연관된 객체들 삭제")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "투표 삭제 완료"),
        @ApiResponse(
                responseCode = "401",
                description = "인증 실패 또는 만료된 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "삭제할 수 있는 권한이 없음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "투표 또는 팀 멤버를 찾을 수 없음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> deleteVote(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable("voteId") Long voteId);

    @Operation(summary = "getMyVotes", description = "내가 투표 대상자로 지정된 투표 목록 조회")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "내 투표 목록 조회 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                array = @ArraySchema(schema = @Schema(implementation = EventVoteResponse.class)))),
        @ApiResponse(
                responseCode = "401",
                description = "인증 실패 또는 만료된 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<List<EventVoteResponse>> getMyVotes(@AuthenticationPrincipal UserDetailsImpl userDetails);
}
