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
@RequestMapping("/api/v1/recruitments")
public class RecruitmentController {

    private final RecruitmentService recruitmentService;
    private final RecruitmentApplicationService recruitmentApplicationService;

    @GetMapping
    public ResponseEntity<Page<RecruitmentSummaryResponse>> getRecruitments(Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(recruitmentService.getRecruitments(pageable));
    }

    @GetMapping("/me")
    public ResponseEntity<Page<RecruitmentSummaryResponse>> getMyRecruitments(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            Pageable pageable
    ) {
        User user = userDetails.getUser();
        return ResponseEntity.status(HttpStatus.OK)
                .body(recruitmentService.getMyRecruitments(user, pageable));
    }

    @GetMapping("/{recruitmentId}")
    public ResponseEntity<RecruitmentDetailResponse> getRecruitment(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long recruitmentId
    ) {
        User user = userDetails.getUser();
        return ResponseEntity.status(HttpStatus.OK)
                .body(recruitmentService.getRecruitment(recruitmentId, user));
    }

    @PostMapping
    public ResponseEntity<RecruitmentDetailResponse> creatRecruitment(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody RecruitmentCreateRequest request
    ) {
        User user = userDetails.getUser();
        return ResponseEntity.status(HttpStatus.OK)
                .body(recruitmentService.createRecruitment(request, user));
    }

    @PutMapping("/{recruitmentId}")
    public ResponseEntity<RecruitmentDetailResponse> updateRecruitment(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long recruitmentId,
            @Valid @RequestBody RecruitmentUpdateRequest request
    ) {
        User user = userDetails.getUser();
        return ResponseEntity.status(HttpStatus.OK)
                .body(recruitmentService.updateRecruitment(recruitmentId, request, user));
    }

    @PostMapping("/{recruitmentId}/applications")
    public ResponseEntity<ApplicationSummaryResponse> apply(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long recruitmentId,
            @Valid @RequestBody ApplicationCreateRequest request
    ) {
        User user = userDetails.getUser();
        return ResponseEntity.status(HttpStatus.OK)
                .body(recruitmentApplicationService.apply(recruitmentId, request, user));
    }

    @GetMapping("/{recruitmentId}/applications")
    public ResponseEntity<Page<ApplicationSummaryResponse>> getApplicationsByRecruitment(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long recruitmentId,
            Pageable pageable
    ) {
        User user = userDetails.getUser();
        return ResponseEntity.status(HttpStatus.OK)
                .body(recruitmentApplicationService.getApplicationsByRecruitment(recruitmentId, user, pageable));
    }
}
