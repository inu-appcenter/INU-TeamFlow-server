package com.inuteamflow.server.domain.infoPost.controller;

import com.inuteamflow.server.domain.infoPost.dto.request.InfoPostCreateRequest;
import com.inuteamflow.server.domain.infoPost.dto.request.InfoPostUpdateRequest;
import com.inuteamflow.server.domain.infoPost.dto.response.InfoPostDetailResponse;
import com.inuteamflow.server.domain.infoPost.dto.response.InfoPostSummaryResponse;
import com.inuteamflow.server.domain.infoPost.enums.InfoPostCategory;
import com.inuteamflow.server.domain.infoPost.enums.InfoPostType;
import com.inuteamflow.server.domain.infoPost.service.InfoPostService;
import com.inuteamflow.server.domain.recruitment.dto.response.RecruitmentSummaryResponse;
import com.inuteamflow.server.domain.report.dto.request.ReportRequest;
import com.inuteamflow.server.domain.report.dto.response.ReportResponse;
import com.inuteamflow.server.domain.report.service.ReportService;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/info-posts")
public class InfoPostController implements InfoPostControllerDocument {

    private final InfoPostService infoPostService;
    private final ReportService reportService;

    @GetMapping
    public ResponseEntity<Page<InfoPostSummaryResponse>> getInfoPosts(
            @RequestParam(required = false) InfoPostCategory category,
            @RequestParam(required = false) InfoPostType type,
            @RequestParam(required = false) String keyword,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(infoPostService.getInfoPosts(category, type, keyword, pageable));
    }

    @GetMapping("/me")
    public ResponseEntity<Page<InfoPostSummaryResponse>> getMyInfoPosts(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(infoPostService.getMyInfoPosts(userDetails.getUser(), pageable));
    }

    @GetMapping("/{infoPostId}")
    public ResponseEntity<InfoPostDetailResponse> getInfoPost(
            @PathVariable Long infoPostId, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(infoPostService.getInfoPost(infoPostId, userDetails.getUser()));
    }

    @PostMapping
    public ResponseEntity<InfoPostDetailResponse> createInfoPost(
            @Valid @RequestBody InfoPostCreateRequest request, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(infoPostService.createInfoPost(request, userDetails.getUser()));
    }

    @PutMapping("/{infoPostId}")
    public ResponseEntity<InfoPostDetailResponse> updateInfoPost(
            @PathVariable Long infoPostId,
            @Valid @RequestBody InfoPostUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(infoPostService.updateInfoPost(infoPostId, request, userDetails.getUser()));
    }

    @DeleteMapping("/{infoPostId}")
    public ResponseEntity<Void> deleteInfoPost(
            @PathVariable Long infoPostId, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        infoPostService.deleteInfoPost(infoPostId, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/{infoPostId}/recruitments")
    public ResponseEntity<List<RecruitmentSummaryResponse>> getRecruitmentsByInfoPost(@PathVariable Long infoPostId) {
        return ResponseEntity.status(HttpStatus.OK).body(infoPostService.getRecruitmentsByInfoPost(infoPostId));
    }

    @PostMapping("/{infoPostId}/scraps")
    public ResponseEntity<Void> scrapInfoPost(
            @PathVariable Long infoPostId, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        infoPostService.scrapInfoPost(infoPostId, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/{infoPostId}/scraps")
    public ResponseEntity<Void> unscrapInfoPost(
            @PathVariable Long infoPostId, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        infoPostService.unscrapInfoPost(infoPostId, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/scraps")
    public ResponseEntity<Slice<InfoPostSummaryResponse>> getMyInfoPostScraps(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(infoPostService.getMyInfoPostScraps(userDetails.getUser(), pageable));
    }

    @PostMapping("/{infoPostId}/reports")
    public ResponseEntity<ReportResponse> reportInfoPost(
            @PathVariable Long infoPostId,
            @Valid @RequestBody ReportRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.reportInfoPost(infoPostId, request, userDetails.getUser()));
    }
}
