package com.inuteamflow.server.domain.admin.controller;

import com.inuteamflow.server.domain.admin.dto.response.DashboardResponse;
import com.inuteamflow.server.domain.admin.service.AdminService;
import com.inuteamflow.server.domain.inquiry.dto.request.InquiryHandleRequest;
import com.inuteamflow.server.domain.inquiry.dto.response.InquiryDetailResponse;
import com.inuteamflow.server.domain.inquiry.dto.response.InquirySummaryResponse;
import com.inuteamflow.server.domain.report.dto.request.ReportHandleRequest;
import com.inuteamflow.server.domain.report.dto.response.ReportDetailResponse;
import com.inuteamflow.server.domain.report.dto.response.ReportSummaryResponse;
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
@RequestMapping("/api/v1/admin")
public class AdminController implements AdminControllerDocument {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getDashboard(pageable, userDetails.getUser()));
    }

    @GetMapping("/reports")
    public ResponseEntity<ReportSummaryResponse> getReports(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getReports(pageable, userDetails.getUser()));
    }

    @GetMapping("/reports/{reportId}")
    public ResponseEntity<ReportDetailResponse> getReport(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable Long reportId) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getReport(reportId, userDetails.getUser()));
    }

    @PatchMapping("/reports/{reportId}")
    public ResponseEntity<Void> handleReport(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long reportId,
            @Valid @RequestBody ReportHandleRequest request) {
        adminService.handleReport(reportId, request, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/inquiries")
    public ResponseEntity<InquirySummaryResponse> getInquiries(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getInquiries(pageable, userDetails.getUser()));
    }

    @GetMapping("/inquiries/{inquiryId}")
    public ResponseEntity<InquiryDetailResponse> getInquiry(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable Long inquiryId) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getInquiry(inquiryId, userDetails.getUser()));
    }

    @PatchMapping("/inquiries/{inquiryId}")
    public ResponseEntity<Void> handleInquiry(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long inquiryId,
            @Valid @RequestBody InquiryHandleRequest request) {
        adminService.handleInquiry(inquiryId, request, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
