package com.inuteamflow.server.domain.fcm.controller;

import com.inuteamflow.server.domain.fcm.dto.req.FcmRequest;
import com.inuteamflow.server.domain.fcm.dto.res.FcmResponse;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "FCM Token Controller", description = "FCM 토큰 컨트롤러")
public interface FcmControllerDocument {

    @Operation(summary = "createFcmToken", description = "FCM 토큰 등록 (이미 등록된 토큰이면 기존 토큰 반환)")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "FCM 토큰 등록 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = FcmResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 값",
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
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<FcmResponse> createFcmToken(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @Valid @RequestBody FcmRequest fcmRequest);

    @Operation(summary = "deleteFcmToken", description = "FCM 토큰 삭제 (로그아웃 또는 알림 수신 거부 시 호출)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "FCM 토큰 삭제 성공"),
        @ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "등록되지 않은 FCM 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> deleteFcmToken(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @Valid @RequestBody FcmRequest fcmRequest);
}
