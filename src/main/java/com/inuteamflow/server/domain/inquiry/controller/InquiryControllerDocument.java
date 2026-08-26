package com.inuteamflow.server.domain.inquiry.controller;

import com.inuteamflow.server.domain.inquiry.dto.request.InquiryRequest;
import com.inuteamflow.server.domain.inquiry.dto.response.InquiryDetailResponse;
import com.inuteamflow.server.domain.inquiry.dto.response.InquiryResponse;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.global.exception.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Inquiry Controller", description = "문의 컨트롤러")
public interface InquiryControllerDocument {

    @Operation(summary = "createInquiry", description = "문의 등록")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "문의 등록 성공",
                    content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = InquiryResponse.class))),
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
    ResponseEntity<InquiryResponse> createInquiry(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @Valid @RequestBody InquiryRequest request);

    @Operation(summary = "getMyInquiries", description = "내 문의 목록 조회 (페이지네이션 없음, 최신순)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "내 문의 목록 조회 성공",
                    content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = InquiryResponse.class)))),
            @ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않거나 만료된 토큰",
                    content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<List<InquiryResponse>> getMyInquiries(@AuthenticationPrincipal UserDetailsImpl userDetails);

    @Operation(summary = "getMyInquiry", description = "내 문의 상세 조회 (본인 문의만 가능)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "문의 상세 조회 성공",
                    content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = InquiryDetailResponse.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않거나 만료된 토큰",
                    content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "본인의 문의가 아님",
                    content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "문의를 찾을 수 없음",
                    content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<InquiryDetailResponse> getMyInquiry(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable Long inquiryId);

    @Operation(summary = "deleteMyInquiry", description = "내 문의 취소 (본인 문의만 가능)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "문의 취소 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않거나 만료된 토큰",
                    content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "본인의 문의가 아님",
                    content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "문의를 찾을 수 없음",
                    content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> deleteMyInquiry(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable Long inquiryId);
}