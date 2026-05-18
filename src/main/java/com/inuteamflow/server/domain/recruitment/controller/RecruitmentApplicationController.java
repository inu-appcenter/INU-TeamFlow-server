package com.inuteamflow.server.domain.recruitment.controller;


import com.inuteamflow.server.domain.recruitment.dto.request.ApplicationStatusUpdateRequest;
import com.inuteamflow.server.domain.recruitment.dto.response.ApplicationDetailResponse;
import com.inuteamflow.server.domain.recruitment.dto.response.ApplicationStatusResponse;
import com.inuteamflow.server.domain.recruitment.dto.response.MyApplicationSummaryResponse;
import com.inuteamflow.server.domain.recruitment.service.RecruitmentApplicationService;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/applications")
public class RecruitmentApplicationController {

    private final RecruitmentApplicationService applicationService;

    @GetMapping("/me")
    public ApiResponse<Page<MyApplicationSummaryResponse>> getMyApplications(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            Pageable pageable
    ) {
        User user = userDetails.getUser();
        return ApiResponse.ok(applicationService.getMyApplications(user, pageable));
    }

    @GetMapping("/{applicationId}")
    public ApiResponse<ApplicationDetailResponse> getApplication(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long applicationId
    ) {
        User user = userDetails.getUser();
        return ApiResponse.ok(applicationService.getApplication(applicationId, user));
    }

    @PutMapping("/{applicationId}/status")
    public ApiResponse<ApplicationStatusResponse> updateApplicationStatus(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long applicationId,
            @Valid @RequestBody ApplicationStatusUpdateRequest request
    ) {
        User user = userDetails.getUser();
        return ApiResponse.ok(applicationService.updateStatus(applicationId, request, user));
    }

}
