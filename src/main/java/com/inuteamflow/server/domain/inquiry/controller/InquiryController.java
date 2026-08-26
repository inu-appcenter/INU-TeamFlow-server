package com.inuteamflow.server.domain.inquiry.controller;

import com.inuteamflow.server.domain.inquiry.dto.request.InquiryRequest;
import com.inuteamflow.server.domain.inquiry.dto.response.InquiryDetailResponse;
import com.inuteamflow.server.domain.inquiry.dto.response.InquiryResponse;
import com.inuteamflow.server.domain.inquiry.service.InquiryService;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/inquiries")
public class InquiryController implements InquiryControllerDocument {

    private final InquiryService inquiryService;

    @PostMapping
    public ResponseEntity<InquiryResponse> createInquiry(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @Valid @RequestBody InquiryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inquiryService.createInquiry(request, userDetails.getUser()));
    }

    @GetMapping("/me")
    public ResponseEntity<List<InquiryResponse>> getMyInquiries(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.OK).body(inquiryService.getMyInquiries(userDetails.getUser()));
    }

    @GetMapping("/{inquiryId}")
    public ResponseEntity<InquiryDetailResponse> getMyInquiry(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable Long inquiryId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(inquiryService.getMyInquiry(inquiryId, userDetails.getUser()));
    }

    @DeleteMapping("/{inquiryId}")
    public ResponseEntity<Void> deleteMyInquiry(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable Long inquiryId) {
        inquiryService.deleteMyInquiry(inquiryId, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}