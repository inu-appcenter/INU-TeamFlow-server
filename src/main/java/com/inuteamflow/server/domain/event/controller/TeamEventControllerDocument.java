package com.inuteamflow.server.domain.event.controller;

import com.inuteamflow.server.domain.event.dto.request.TeamEventCreateRequest;
import com.inuteamflow.server.domain.event.dto.request.TeamEventUpdateRequest;
import com.inuteamflow.server.domain.event.dto.response.EventDetailResponse;
import com.inuteamflow.server.domain.event.dto.response.EventListResponse;
import com.inuteamflow.server.domain.event.enums.RecurrenceEditScope;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.global.exception.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Team Event Controller", description = "팀 일정 컨트롤러")
public interface TeamEventControllerDocument {

    @Operation(summary = "getTeamEventList", description = "팀 일정 목록 조회")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "팀 일정 목록 조회 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                array = @ArraySchema(schema = @Schema(implementation = EventListResponse.class)))),
        @ApiResponse(
                responseCode = "400",
                description = "잘못된 year 또는 month 값",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 토큰",
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
                description = "팀 또는 팀원을 찾을 수 없음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<List<EventListResponse>> getTeamEventList(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable("teamId") Long teamId,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month);

    @Operation(summary = "createEvent", description = "팀 일정 생성")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "팀 일정 생성 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = EventDetailResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 값 또는 유효하지 않은 일정 참여자",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "팀 일정을 관리할 권한이 없거나 정지된 사용자",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "팀, 팀원 또는 참여자를 찾을 수 없음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<EventDetailResponse> createEvent(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable("teamId") Long teamId,
            @Valid @RequestBody TeamEventCreateRequest request);

    @Operation(summary = "updateTeamEvent", description = "팀 일정 수정")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "팀 일정 수정 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = EventDetailResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 값, 팀-일정 불일치, 반복 일정 정보 누락 또는 유효하지 않은 일정 참여자",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "팀 일정을 관리할 권한이 없거나 정지된 사용자",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "일정, 팀, 팀원, 반복 규칙, 대상 반복 일정 또는 호스트 참여자를 찾을 수 없음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<EventDetailResponse> updateTeamEvent(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable("teamId") Long teamId,
            @PathVariable("eventId") Long eventId,
            @Valid @RequestBody TeamEventUpdateRequest request);

    @Operation(summary = "deleteTeamEvent", description = "팀 일정 삭제")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "팀 일정 삭제 성공"),
        @ApiResponse(
                responseCode = "400",
                description = "팀과 일정 정보가 일치하지 않음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "팀 일정을 관리할 권한이 없거나 정지된 사용자",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "일정, 팀, 팀원, 반복 규칙 또는 대상 반복 일정을 찾을 수 없음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> deleteTeamEvent(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable("teamId") Long teamId,
            @PathVariable("eventId") Long eventId,
            @RequestParam(name = "scope", required = false) RecurrenceEditScope recurrenceEditScope,
            @RequestParam(name = "occurrence", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime occurrenceAt);
}
