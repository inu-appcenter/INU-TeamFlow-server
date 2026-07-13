package com.inuteamflow.server.domain.fcm.controller;

import com.inuteamflow.server.domain.fcm.dto.req.FcmRequest;
import com.inuteamflow.server.domain.fcm.dto.res.FcmResponse;
import com.inuteamflow.server.domain.fcm.service.FcmService;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/fcm")
public class FcmController implements FcmControllerDocument {

    private final FcmService fcmService;

    @PostMapping
    public ResponseEntity<FcmResponse> createFcmToken(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody FcmRequest fcmRequest
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(fcmService.createFcmToken(userDetails.getUser(), fcmRequest));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteFcmToken(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody FcmRequest fcmRequest
    ) {
        fcmService.deleteFcmToken(userDetails.getUser(), fcmRequest);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}
