package com.inuteamflow.server.domain.teamNotice.controller;

import com.inuteamflow.server.domain.teamNotice.dto.req.TeamNoticeCreateRequest;
import com.inuteamflow.server.domain.teamNotice.dto.req.TeamNoticeUpdateRequest;
import com.inuteamflow.server.domain.teamNotice.dto.res.TeamNoticeDetailResponse;
import com.inuteamflow.server.domain.teamNotice.dto.res.TeamNoticeSummaryResponse;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.global.exception.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "TeamNotice Controller", description = "팀 공지 컨트롤러")
public interface TeamNoticeControllerDocument {

	@Operation(summary = "getTeamNotices", description = "팀 공지 목록 조회")
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "팀 공지 목록 조회 성공",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = TeamNoticeSummaryResponse.class)
					)
			),
			@ApiResponse(
					responseCode = "401",
					description = "인증 실패 또는 만료된 토큰",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ErrorResponse.class)
					)
			),
			@ApiResponse(
					responseCode = "404",
					description = "팀 또는 팀 멤버를 찾을 수 없음",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ErrorResponse.class)
					)
			)
	})
	ResponseEntity<Page<TeamNoticeSummaryResponse>> getTeamNotices(
			@PathVariable Long teamId,
			@AuthenticationPrincipal UserDetailsImpl userDetails,
			Pageable pageable
	);

	@Operation(summary = "getTeamNotice", description = "팀 공지 상세 조회 (읽음 처리 포함)")
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "팀 공지 상세 조회 성공",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = TeamNoticeDetailResponse.class)
					)
			),
			@ApiResponse(
					responseCode = "401",
					description = "인증 실패 또는 만료된 토큰",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ErrorResponse.class)
					)
			),
			@ApiResponse(
					responseCode = "404",
					description = "팀, 팀 멤버 또는 공지를 찾을 수 없음",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ErrorResponse.class)
					)
			)
	})
	ResponseEntity<TeamNoticeDetailResponse> getTeamNotice(
			@PathVariable Long teamId,
			@PathVariable Long noticeId,
			@AuthenticationPrincipal UserDetailsImpl userDetails
	);

	@Operation(summary = "createTeamNotice", description = "팀 공지 생성")
	@ApiResponses({
			@ApiResponse(
					responseCode = "201",
					description = "팀 공지 생성 성공",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = TeamNoticeDetailResponse.class)
					)
			),
			@ApiResponse(
					responseCode = "400",
					description = "잘못된 요청 값",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ErrorResponse.class)
					)
			),
			@ApiResponse(
					responseCode = "401",
					description = "인증 실패 또는 만료된 토큰",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ErrorResponse.class)
					)
			),
			@ApiResponse(
					responseCode = "404",
					description = "팀 또는 팀 멤버를 찾을 수 없음",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ErrorResponse.class)
					)
			)
	})
	ResponseEntity<TeamNoticeDetailResponse> createTeamNotice(
			@PathVariable Long teamId,
			@Valid @RequestBody TeamNoticeCreateRequest request,
			@AuthenticationPrincipal UserDetailsImpl userDetails
	);

	@Operation(summary = "updateTeamNotice", description = "팀 공지 수정 (작성자, LEADER, MANAGER 가능)")
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "팀 공지 수정 성공",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = TeamNoticeDetailResponse.class)
					)
			),
			@ApiResponse(
					responseCode = "400",
					description = "잘못된 요청 값",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ErrorResponse.class)
					)
			),
			@ApiResponse(
					responseCode = "401",
					description = "인증 실패 또는 만료된 토큰",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ErrorResponse.class)
					)
			),
			@ApiResponse(
					responseCode = "403",
					description = "공지 수정 권한 없음",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ErrorResponse.class)
					)
			),
			@ApiResponse(
					responseCode = "404",
					description = "팀, 팀 멤버 또는 공지를 찾을 수 없음",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ErrorResponse.class)
					)
			)
	})
	ResponseEntity<TeamNoticeDetailResponse> updateTeamNotice(
			@PathVariable Long teamId,
			@PathVariable Long noticeId,
			@Valid @RequestBody TeamNoticeUpdateRequest request,
			@AuthenticationPrincipal UserDetailsImpl userDetails
	);

	@Operation(summary = "deleteTeamNotice", description = "팀 공지 삭제 (작성자, LEADER, MANAGER 가능)")
	@ApiResponses({
			@ApiResponse(
					responseCode = "204",
					description = "팀 공지 삭제 성공"
			),
			@ApiResponse(
					responseCode = "401",
					description = "인증 실패 또는 만료된 토큰",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ErrorResponse.class)
					)
			),
			@ApiResponse(
					responseCode = "403",
					description = "공지 삭제 권한 없음",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ErrorResponse.class)
					)
			),
			@ApiResponse(
					responseCode = "404",
					description = "팀, 팀 멤버 또는 공지를 찾을 수 없음",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ErrorResponse.class)
					)
			)
	})
	ResponseEntity<Void> deleteTeamNotice(
			@PathVariable Long teamId,
			@PathVariable Long noticeId,
			@AuthenticationPrincipal UserDetailsImpl userDetails
	);

	@Operation(summary = "getMyTeamNotices", description = "내가 속한 팀의 전체 공지 목록 조회")
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "내 팀 공지 목록 조회 성공",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = TeamNoticeSummaryResponse.class)
					)
			),
			@ApiResponse(
					responseCode = "401",
					description = "인증 실패 또는 만료된 토큰",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ErrorResponse.class)
					)
			)
	})
	ResponseEntity<Page<TeamNoticeSummaryResponse>> getMyTeamNotices(
			@AuthenticationPrincipal UserDetailsImpl userDetails,
			Pageable pageable
	);
}
