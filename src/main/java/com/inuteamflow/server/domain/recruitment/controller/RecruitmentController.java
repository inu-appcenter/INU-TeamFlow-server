package com.inuteamflow.server.domain.recruitment.controller;

import com.inuteamflow.server.domain.recruitment.dto.request.ApplicationCreateRequest;
import com.inuteamflow.server.domain.recruitment.dto.request.RecruitmentCreateRequest;
import com.inuteamflow.server.domain.recruitment.dto.request.RecruitmentUpdateRequest;
import com.inuteamflow.server.domain.recruitment.dto.response.ApplicationSummaryResponse;
import com.inuteamflow.server.domain.recruitment.dto.response.RecruitmentDetailResponse;
import com.inuteamflow.server.domain.recruitment.dto.response.RecruitmentSummaryResponse;
import com.inuteamflow.server.domain.recruitment.service.RecruitmentApplicationService;
import com.inuteamflow.server.domain.recruitment.service.RecruitmentService;
import com.inuteamflow.server.domain.report.dto.request.ReportRequest;
import com.inuteamflow.server.domain.report.dto.response.ReportResponse;
import com.inuteamflow.server.domain.report.service.ReportService;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recruitments")
public class RecruitmentController implements RecruitmentControllerDocument {

    private final RecruitmentService recruitmentService;
    private final RecruitmentApplicationService recruitmentApplicationService;
    private final ReportService reportService;

    @GetMapping
    public ResponseEntity<Page<RecruitmentSummaryResponse>> getRecruitments(
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(recruitmentService.getRecruitments(pageable));
    }

    @GetMapping("/me")
    public ResponseEntity<Page<RecruitmentSummaryResponse>> getMyRecruitments(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        User user = userDetails.getUser();
        return ResponseEntity.status(HttpStatus.OK).body(recruitmentService.getMyRecruitments(user, pageable));
    }

    @GetMapping("/{recruitmentId}")
    public ResponseEntity<RecruitmentDetailResponse> getRecruitment(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable Long recruitmentId) {
        User user = userDetails.getUser();
        return ResponseEntity.status(HttpStatus.OK).body(recruitmentService.getRecruitment(recruitmentId, user));
    }

    @PostMapping
    public ResponseEntity<RecruitmentDetailResponse> creatRecruitment(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody RecruitmentCreateRequest request) {
        User user = userDetails.getUser();
        return ResponseEntity.status(HttpStatus.OK).body(recruitmentService.createRecruitment(request, user));
    }

    @PutMapping("/{recruitmentId}")
    public ResponseEntity<RecruitmentDetailResponse> updateRecruitment(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long recruitmentId,
            @Valid @RequestBody RecruitmentUpdateRequest request) {
        User user = userDetails.getUser();
        return ResponseEntity.status(HttpStatus.OK)
                .body(recruitmentService.updateRecruitment(recruitmentId, request, user));
    }

    @DeleteMapping("/{recruitmentId}")
    public ResponseEntity<Void> deleteRecruitment(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable Long recruitmentId) {
        User user = userDetails.getUser();
        recruitmentService.deleteRecruitment(recruitmentId, user);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/{recruitmentId}/applications")
    public ResponseEntity<ApplicationSummaryResponse> apply(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long recruitmentId,
            @Valid @RequestBody ApplicationCreateRequest request) {
        User user = userDetails.getUser();
        return ResponseEntity.status(HttpStatus.OK)
                .body(recruitmentApplicationService.apply(recruitmentId, request, user));
    }

    @GetMapping("/{recruitmentId}/applications")
    public ResponseEntity<Page<ApplicationSummaryResponse>> getApplicationsByRecruitment(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long recruitmentId,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        User user = userDetails.getUser();
        return ResponseEntity.status(HttpStatus.OK)
                .body(recruitmentApplicationService.getApplicationsByRecruitment(recruitmentId, user, pageable));
    }

    @PostMapping("/{recruitmentId}/scraps")
    public ResponseEntity<Void> scrapRecruitment(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable Long recruitmentId) {
        recruitmentService.scrapRecruitment(recruitmentId, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/{recruitmentId}/scraps")
    public ResponseEntity<Void> unscrapRecruitment(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable Long recruitmentId) {
        recruitmentService.unscrapRecruitment(recruitmentId, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/scraps")
    public ResponseEntity<Slice<RecruitmentSummaryResponse>> getMyRecruitmentScraps(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(recruitmentService.getMyRecruitmentScraps(userDetails.getUser(), pageable));
    }

    @PostMapping("/{recruitmentId}/reports")
    public ResponseEntity<ReportResponse> reportRecruitment(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long recruitmentId,
            @Valid @RequestBody ReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.reportRecruitment(recruitmentId, request, userDetails.getUser()));
    }
}
