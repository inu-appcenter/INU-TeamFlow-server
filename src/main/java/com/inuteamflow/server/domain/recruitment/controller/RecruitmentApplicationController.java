package com.inuteamflow.server.domain.recruitment.controller;


import com.inuteamflow.server.domain.recruitment.dto.request.ApplicationStatusUpdateRequest;
import com.inuteamflow.server.domain.recruitment.dto.response.ApplicationDetailResponse;
import com.inuteamflow.server.domain.recruitment.dto.response.ApplicationStatusResponse;
import com.inuteamflow.server.domain.recruitment.dto.response.MyApplicationSummaryResponse;
import com.inuteamflow.server.domain.recruitment.service.RecruitmentApplicationService;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/applications")
public class RecruitmentApplicationController {

    private final RecruitmentApplicationService applicationService;

    @GetMapping("/me")
    public ResponseEntity<Page<MyApplicationSummaryResponse>> getMyApplications(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            Pageable pageable
    ) {
        User user = userDetails.getUser();
        return ResponseEntity.status(HttpStatus.OK)
                .body(applicationService.getMyApplications(user, pageable));
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<ApplicationDetailResponse> getApplication(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long applicationId
    ) {
        User user = userDetails.getUser();
        return ResponseEntity.status(HttpStatus.OK)
                .body(applicationService.getApplication(applicationId, user));
    }

    @PutMapping("/{applicationId}/status")
    public ResponseEntity<ApplicationStatusResponse> updateApplicationStatus(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long applicationId,
            @Valid @RequestBody ApplicationStatusUpdateRequest request
    ) {
        User user = userDetails.getUser();
        return ResponseEntity.status(HttpStatus.OK)
                .body(applicationService.updateStatus(applicationId, request, user));
    }

}
