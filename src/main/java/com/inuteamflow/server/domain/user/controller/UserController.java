package com.inuteamflow.server.domain.user.controller;

import com.inuteamflow.server.domain.report.dto.request.ReportRequest;
import com.inuteamflow.server.domain.report.dto.response.ReportResponse;
import com.inuteamflow.server.domain.report.service.ReportService;
import com.inuteamflow.server.domain.user.dto.request.UserUpdateRequest;
import com.inuteamflow.server.domain.user.dto.response.MyInfoResponse;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController implements UserControllerDocument {

    private final UserService userService;
    private final ReportService reportService;

    @GetMapping("/me")
    public ResponseEntity<MyInfoResponse> getMyInfo(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getMyInfo(userDetails.getUser()));
    }

    @PutMapping("/me")
    public ResponseEntity<MyInfoResponse> updateMyInfo(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.updateMyInfo(userDetails.getUser(), request));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        userService.deleteUser(userDetails.getUser());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/{userId}/reports")
    public ResponseEntity<ReportResponse> reportUser(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long userId,
            @Valid @RequestBody ReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.reportUser(userId, request, userDetails.getUser()));
    }
}
