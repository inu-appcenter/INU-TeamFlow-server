package com.inuteamflow.server.domain.user.controller;

import com.inuteamflow.server.domain.user.dto.request.LoginRequest;
import com.inuteamflow.server.domain.user.dto.request.SignupRequest;
import com.inuteamflow.server.domain.user.dto.request.VerifySchoolRequest;
import com.inuteamflow.server.domain.user.dto.response.MyInfoResponse;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.global.exception.error.ErrorResponse;
import com.inuteamflow.server.global.jwt.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Auth Controller", description = "인증 컨트롤러")
public interface AuthControllerDocument {

    @Operation(summary = "login", description = "로그인. X-Client-Type: mobile 헤더를 보내면 리프레시 토큰 유효기간이 30일, 없으면 3일")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "로그인 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = TokenResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 값",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "아이디 또는 비밀번호 불일치",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "정지된 사용자",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<TokenResponse> login(
            @Parameter(description = "클라이언트 타입 (mobile이면 모바일, 미입력 시 웹)", example = "mobile")
                    @RequestHeader(value = "X-Client-Type", required = false)
                    String clientType,
            @Valid @RequestBody LoginRequest loginRequest);

    @Operation(summary = "reissue", description = "토큰 재발급. 로그인할 때의 클라이언트 타입 기준으로 유효기간이 다시 시작됨")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "토큰 재발급 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = TokenResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "저장된 토큰과 일치하지 않음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "리프레시 토큰 쿠키가 없거나, 만료·폐기되었거나, 유효하지 않음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "사용자 정보를 찾을 수 없음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<TokenResponse> reissue(@CookieValue("refreshToken") String refreshToken);

    @Operation(summary = "logout", description = "로그아웃 (리프레시 토큰 무효화 및 쿠키 삭제)")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "로그아웃 성공")})
    ResponseEntity<Void> logout(@CookieValue(value = "refreshToken", required = false) String refreshToken);

    @Operation(summary = "signup", description = "회원가입")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "회원가입 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = MyInfoResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 값",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "아이디 또는 이메일 중복",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<MyInfoResponse> signup(@Valid @RequestBody SignupRequest request);

    @Operation(summary = "verifySchool", description = "학교 인증")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "학교 인증 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = MyInfoResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 값 또는 학교 인증 실패",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "인증 실패 또는 만료된 토큰",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "정지된 사용자",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "사용자를 찾을 수 없음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "503",
                description = "학교 인증 서비스를 사용할 수 없음",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<MyInfoResponse> verifySchool(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @Valid @RequestBody VerifySchoolRequest request);
}
