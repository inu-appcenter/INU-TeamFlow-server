package com.inuteamflow.server.domain.notification.controller;

import com.inuteamflow.server.domain.notification.dto.req.NotificationRequest;
import com.inuteamflow.server.domain.notification.dto.res.NotificationSliceResponse;
import com.inuteamflow.server.domain.notification.enums.NotificationType;
import com.inuteamflow.server.domain.notification.service.NotificationService;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController implements NotificationControllerDocument {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<NotificationSliceResponse> getNotifications(
            @RequestParam(required = false) NotificationType type,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(notificationService.getNotifications(type, userDetails.getUser(), pageable));
    }

    @PatchMapping("/{notificationId}")
    public ResponseEntity<Void> readNotification(
            @PathVariable Long notificationId, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        notificationService.readNotification(notificationId, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PatchMapping
    public ResponseEntity<Void> readNotifications(
            @Valid @RequestBody NotificationRequest request, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        notificationService.readNotifications(request, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteNotifications(
            @Valid @RequestBody NotificationRequest request, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        notificationService.deleteNotifications(request, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
