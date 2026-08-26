package com.inuteamflow.server.domain.infoPost.controller;

import com.inuteamflow.server.domain.infoPost.dto.request.InfoPostCreateRequest;
import com.inuteamflow.server.domain.infoPost.dto.request.InfoPostUpdateRequest;
import com.inuteamflow.server.domain.infoPost.dto.response.InfoPostDetailResponse;
import com.inuteamflow.server.domain.infoPost.dto.response.InfoPostSummaryResponse;
import com.inuteamflow.server.domain.infoPost.enums.InfoPostCategory;
import com.inuteamflow.server.domain.infoPost.enums.InfoPostType;
import com.inuteamflow.server.domain.recruitment.dto.response.RecruitmentSummaryResponse;
import com.inuteamflow.server.domain.report.dto.request.ReportRequest;
import com.inuteamflow.server.domain.report.dto.response.ReportResponse;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "InfoPost Controller", description = "정보글 컨트롤러")
public interface InfoPostControllerDocument {

    @Operation(summary = "getInfoPosts", description = "정보글 목록 조회 (category/type/keyword 필터 가능)")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "정보글 목록 조회 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = InfoPostSummaryResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Page<InfoPostSummaryResponse>> getInfoPosts(
            @RequestParam(required = false) InfoPostCategory category,
            @RequestParam(required = false) InfoPostType type,
            @RequestParam(required = false) String keyword,
            Pageable pageable);

    @Operation(summary = "getMyInfoPosts", description = "내가 작성한 정보글 목록 조회")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "내 정보글 목록 조회 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = InfoPostSummaryResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Page<InfoPostSummaryResponse>> getMyInfoPosts(
            @AuthenticationPrincipal UserDetailsImpl userDetails, Pageable pageable);

    @Operation(summary = "getInfoPost", description = "정보글 상세 조회")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "정보글 상세 조회 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = InfoPostDetailResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "정보글을 찾을 수 없음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<InfoPostDetailResponse> getInfoPost(
            @PathVariable Long infoPostId, @AuthenticationPrincipal UserDetailsImpl userDetails);

    @Operation(summary = "createInfoPost", description = "정보글 작성")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "정보글 작성 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = InfoPostDetailResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 값",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<InfoPostDetailResponse> createInfoPost(
            @Valid @RequestBody InfoPostCreateRequest request, @AuthenticationPrincipal UserDetailsImpl userDetails);

    @Operation(summary = "updateInfoPost", description = "정보글 수정 (작성자만 가능)")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "정보글 수정 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = InfoPostDetailResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 값",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "작성자가 아님",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "정보글을 찾을 수 없음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<InfoPostDetailResponse> updateInfoPost(
            @PathVariable Long infoPostId,
            @Valid @RequestBody InfoPostUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails);

    @Operation(summary = "deleteInfoPost", description = "정보글 삭제 (작성자만 가능)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "정보글 삭제 성공"),
        @ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "작성자가 아님",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "정보글을 찾을 수 없음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> deleteInfoPost(
            @PathVariable Long infoPostId, @AuthenticationPrincipal UserDetailsImpl userDetails);

    @Operation(summary = "getRecruitmentsByInfoPost", description = "이 정보글을 참조하는 모집글 전체 목록 조회 (페이징 없음, 최신순)")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "모집글 목록 조회 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                array =
                                        @ArraySchema(
                                                schema = @Schema(implementation = RecruitmentSummaryResponse.class)))),
        @ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "정보글을 찾을 수 없음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<List<RecruitmentSummaryResponse>> getRecruitmentsByInfoPost(@PathVariable Long infoPostId);

    @Operation(summary = "scrapInfoPost", description = "정보글 스크랩")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "스크랩 성공"),
        @ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "정보글을 찾을 수 없음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "이미 스크랩한 정보글",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> scrapInfoPost(
            @PathVariable Long infoPostId, @AuthenticationPrincipal UserDetailsImpl userDetails);

    @Operation(summary = "unscrapInfoPost", description = "정보글 스크랩 취소")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "스크랩 취소 성공"),
        @ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "정보글을 찾을 수 없거나 스크랩하지 않은 정보글",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> unscrapInfoPost(
            @PathVariable Long infoPostId, @AuthenticationPrincipal UserDetailsImpl userDetails);

    @Operation(summary = "getMyInfoPostScraps", description = "내가 스크랩한 정보글 목록 조회 (스크랩한 시각 최신순)")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "스크랩한 정보글 목록 조회 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = InfoPostSummaryResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Slice<InfoPostSummaryResponse>> getMyInfoPostScraps(
            @AuthenticationPrincipal UserDetailsImpl userDetails, Pageable pageable);

    @Operation(summary = "reportInfoPost", description = "정보글 신고")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "신고 접수 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ReportResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 값",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "정보글을 찾을 수 없음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ReportResponse> reportInfoPost(
            @PathVariable Long infoPostId,
            @Valid @RequestBody ReportRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails);
}
