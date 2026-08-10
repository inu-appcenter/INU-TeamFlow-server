package com.inuteamflow.server.domain.notification.controller;

import com.inuteamflow.server.domain.notification.dto.req.NotificationOptionRequest;
import com.inuteamflow.server.domain.notification.dto.res.NotificationOptionResponse;
import com.inuteamflow.server.domain.notification.service.NotificationOptionService;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notification-options")
public class NotificationOptionController implements NotificationOptionControllerDocument {

    private final NotificationOptionService notificationOptionService;

    @GetMapping
    public ResponseEntity<NotificationOptionResponse> getNotificationOptions(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(notificationOptionService.getNotificationOption(userDetails.getUser()));
    }

    @PostMapping
    public ResponseEntity<NotificationOptionResponse> createNotificationOption(
            @Valid @RequestBody NotificationOptionRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationOptionService.createNotificationOption(request, userDetails.getUser()));
    }

    @PutMapping
    public ResponseEntity<NotificationOptionResponse> updateNotificationOption(
            @Valid @RequestBody NotificationOptionRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(notificationOptionService.updateNotificationOption(request, userDetails.getUser()));
    }
}
