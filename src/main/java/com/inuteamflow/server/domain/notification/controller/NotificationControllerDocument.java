package com.inuteamflow.server.domain.notification.controller;

import com.inuteamflow.server.domain.notification.dto.req.NotificationRequest;
import com.inuteamflow.server.domain.notification.dto.res.NotificationSliceResponse;
import com.inuteamflow.server.domain.notification.enums.NotificationType;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Notification Controller", description = "알림 컨트롤러")
public interface NotificationControllerDocument {

    @Operation(summary = "getNotifications", description = "알림 목록 조회 (무한스크롤, 타입 필터 가능)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "알림 목록 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NotificationSliceResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않거나 만료된 토큰",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    ResponseEntity<NotificationSliceResponse> getNotifications(
            @RequestParam(required = false) NotificationType type,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ParameterObject Pageable pageable
    );

    @Operation(summary = "readNotification", description = "알림 단건 읽음 처리")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "알림 읽음 처리 성공"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않거나 만료된 토큰",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "알림 수신자가 아님",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "알림을 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    ResponseEntity<Void> readNotification(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    );

    @Operation(summary = "readNotifications", description = "알림 선택 읽음 처리")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "알림 선택 읽음 처리 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "알림 ID 목록이 비어있음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않거나 만료된 토큰",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    ResponseEntity<Void> readNotifications(
            @Valid @RequestBody NotificationRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    );

    @Operation(summary = "deleteNotifications", description = "알림 선택 삭제")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "알림 선택 삭제 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "알림 ID 목록이 비어있음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않거나 만료된 토큰",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    ResponseEntity<Void> deleteNotifications(
            @Valid @RequestBody NotificationRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    );
}
