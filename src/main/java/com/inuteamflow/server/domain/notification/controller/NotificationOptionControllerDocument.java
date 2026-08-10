package com.inuteamflow.server.domain.notification.controller;

import com.inuteamflow.server.domain.notification.dto.req.NotificationOptionRequest;
import com.inuteamflow.server.domain.notification.dto.res.NotificationOptionResponse;
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

@Tag(name = "Notification Option Controller", description = "알림 활성화 옵션 컨트롤러")
public interface NotificationOptionControllerDocument {

    @Operation(summary = "getNotificationOptions", description = "알림 활성화 옵션 조회")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "알림 활성화 옵션 조회 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = NotificationOptionResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "알림 활성화 옵션을 찾을 수 없음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<NotificationOptionResponse> getNotificationOptions(
            @AuthenticationPrincipal UserDetailsImpl userDetails);

    @Operation(summary = "createNotificationOption", description = "알림 활성화 옵션 생성 (회원가입 시 요청)")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "알림 활성화 옵션 생성 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = NotificationOptionResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "알림 활성화 옵션 값이 누락됨",
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
                responseCode = "409",
                description = "이미 알림 활성화 옵션이 존재함",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<NotificationOptionResponse> createNotificationOption(
            @Valid @RequestBody NotificationOptionRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails);

    @Operation(summary = "updateNotificationOption", description = "알림 활성화 옵션 수정")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "알림 활성화 옵션 수정 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = NotificationOptionResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "알림 활성화 옵션 값이 누락됨",
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
                description = "알림 활성화 옵션을 찾을 수 없음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<NotificationOptionResponse> updateNotificationOption(
            @Valid @RequestBody NotificationOptionRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails);
}
