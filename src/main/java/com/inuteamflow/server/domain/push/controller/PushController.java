package com.inuteamflow.server.domain.push.controller;

import com.inuteamflow.server.domain.push.dto.req.PushTokenRequest;
import com.inuteamflow.server.domain.push.dto.res.PushTokenResponse;
import com.inuteamflow.server.domain.push.service.PushService;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/v1/push-tokens", "/api/v1/fcm"})  // TODO: 프론트 전환 후 /fcm 제거
public class PushController implements PushControllerDocument {

    private final PushService pushService;

    @PostMapping
    public ResponseEntity<PushTokenResponse> createPushToken(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody PushTokenRequest pushTokenRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pushService.createPushToken(userDetails.getUser(), pushTokenRequest));
    }

    @DeleteMapping
    public ResponseEntity<Void> deletePushToken(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody PushTokenRequest pushTokenRequest) {
        pushService.deletePushToken(userDetails.getUser(), pushTokenRequest);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
