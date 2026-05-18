package com.inuteamflow.server.domain.recruitment.controller;


import com.inuteamflow.server.domain.recruitment.dto.request.ApplicationCreateRequest;
import com.inuteamflow.server.domain.recruitment.dto.request.RecruitmentCreateRequest;
import com.inuteamflow.server.domain.recruitment.dto.request.RecruitmentUpdateRequest;
import com.inuteamflow.server.domain.recruitment.dto.response.ApplicationSummaryResponse;
import com.inuteamflow.server.domain.recruitment.dto.response.RecruitmentDetailResponse;
import com.inuteamflow.server.domain.recruitment.dto.response.RecruitmentSummaryResponse;
import com.inuteamflow.server.domain.recruitment.service.RecruitmentApplicationService;
import com.inuteamflow.server.domain.recruitment.service.RecruitmentService;
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
@RequestMapping("/api/v1/recruitments")
public class RecruitmentController {

    private final RecruitmentService recruitmentService;
    private final RecruitmentApplicationService recruitmentApplicationService;

    @GetMapping
    public ApiResponse<Page<RecruitmentSummaryResponse>> getRecruitments(Pageable pageable) {
        return ApiResponse.ok(recruitmentService.getRecruitments(pageable));
    }

    @GetMapping("/me")
    public ApiResponse<Page<RecruitmentSummaryResponse>> getMyRecruitments(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            Pageable pageable
    ) {
        User user = userDetails.getUser();
        return ApiResponse.ok(recruitmentService.getMyRecruitments(user, pageable));
    }

    @GetMapping("/{recruitmentId}")
    public ApiResponse<RecruitmentDetailResponse> getRecruitment(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long recruitmentId
    ) {
        User user = userDetails.getUser();
        return ApiResponse.ok(recruitmentService.getRecruitment(recruitmentId, user));
    }

    @PostMapping
    public ApiResponse<RecruitmentDetailResponse> creatRecruitment(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody RecruitmentCreateRequest request
    ) {
        User user = userDetails.getUser();
        return ApiResponse.ok(recruitmentService.createRecruitment(request, user));
    }

    @PutMapping("/{recruitmentId}")
    public ApiResponse<RecruitmentDetailResponse> updateRecruitment(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long recruitmentId,
            @Valid @RequestBody RecruitmentUpdateRequest request
    ) {
        User user = userDetails.getUser();
        return ApiResponse.ok(recruitmentService.updateRecruitment(recruitmentId, request, user));
    }

    @PostMapping("/{recruitmentId}/applications")
    public ApiResponse<ApplicationSummaryResponse> apply(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long recruitmentId,
            @Valid @RequestBody ApplicationCreateRequest request
    ) {
        User user = userDetails.getUser();
        return ApiResponse.ok(recruitmentApplicationService.apply(recruitmentId, request, user));
    }

    @GetMapping("/{recruitmentId}/applications")
    public ApiResponse<Page<ApplicationSummaryResponse>> getApplicationsByRecruitment(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long recruitmentId,
            Pageable pageable
    ) {
        User user = userDetails.getUser();
        return ApiResponse.ok(recruitmentApplicationService.getApplicationsByRecruitment(recruitmentId, user, pageable));
    }
}
