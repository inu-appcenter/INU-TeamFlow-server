package com.inuteamflow.server.domain.invitation.controller;

import com.inuteamflow.server.domain.invitation.dto.request.TeamInvitationCreateRequest;
import com.inuteamflow.server.domain.invitation.dto.request.TeamInvitationStatusUpdateRequest;
import com.inuteamflow.server.domain.invitation.dto.response.InvitationCandidateResponse;
import com.inuteamflow.server.domain.invitation.dto.response.TeamInvitationResponse;
import com.inuteamflow.server.domain.invitation.enums.InvitationDirection;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.global.exception.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Team Invitation Controller", description = "팀 초대 컨트롤러")
public interface TeamInvitationControllerDocument {

    @Operation(summary = "getInvitations", description = "내가 받은/보낸 팀 초대 목록 조회")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "팀 초대 목록 조회 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = TeamInvitationResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Page<TeamInvitationResponse>> getInvitations(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam InvitationDirection direction,
            Pageable pageable);

    @Operation(summary = "invite", description = "팀 초대하기")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "팀 초대 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = TeamInvitationResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 값 / 본인 초대 / 이미 팀원 / 이미 초대 전송 / 학교 미인증 사용자",
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
                description = "팀 리더가 아님",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "팀 또는 사용자를 찾을 수 없음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<TeamInvitationResponse> invite(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long teamId,
            @Valid @RequestBody TeamInvitationCreateRequest request);

    @Operation(summary = "getCandidates", description = "이름으로 팀 초대 후보와 현재 초대 상태 조회")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "팀 초대 후보 조회 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                array =
                                        @ArraySchema(
                                                schema = @Schema(implementation = InvitationCandidateResponse.class)))),
        @ApiResponse(
                responseCode = "400",
                description = "필수 파라미터 누락 (name)",
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
                responseCode = "404",
                description = "팀을 찾을 수 없거나 사용자가 해당 팀의 멤버가 아님",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<List<InvitationCandidateResponse>> getCandidates(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "초대 후보를 검색할 팀 ID", required = true, example = "1") @PathVariable Long teamId,
            @Parameter(description = "검색할 사용자 이름", required = true, example = "손동민") @RequestParam("name") String name);

    @Operation(summary = "updateStatus", description = "팀 초대 수락/거절")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "팀 초대 상태 변경 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = TeamInvitationResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 값 또는 이미 처리된 초대",
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
                description = "초대 받은 본인이 아님",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "초대를 찾을 수 없음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<TeamInvitationResponse> updateStatus(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long invitationId,
            @Valid @RequestBody TeamInvitationStatusUpdateRequest request);

    @Operation(summary = "cancelInvitation", description = "팀 초대 취소")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "팀 초대 취소 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = TeamInvitationResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "팀 리더가 아님",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "팀 또는 초대를 찾을 수 없음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<TeamInvitationResponse> cancelInvitation(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long teamId,
            @PathVariable Long invitationId);
}
