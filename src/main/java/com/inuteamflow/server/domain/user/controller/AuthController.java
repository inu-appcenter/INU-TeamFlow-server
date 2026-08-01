package com.inuteamflow.server.domain.user.controller;

import com.inuteamflow.server.domain.user.dto.request.LoginRequest;
import com.inuteamflow.server.domain.user.dto.request.SignupRequest;
import com.inuteamflow.server.domain.user.dto.request.VerifySchoolRequest;
import com.inuteamflow.server.domain.user.dto.response.MyInfoResponse;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.domain.user.service.AuthService;
import com.inuteamflow.server.global.jwt.JwtTokenProvider;
import com.inuteamflow.server.global.jwt.TokenResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController implements AuthControllerDocument {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        TokenResponse tokenResponse = authService.login(loginRequest);
        return ResponseEntity.status(HttpStatus.OK)
                .header(
                        HttpHeaders.SET_COOKIE,
                        createRefreshTokenCookie(tokenResponse.getRefreshToken())
                                .toString())
                .body(tokenResponse);
    }

    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(@CookieValue(REFRESH_TOKEN_COOKIE_NAME) String refreshToken) {
        TokenResponse tokenResponse = authService.reissue(refreshToken);
        return ResponseEntity.status(HttpStatus.OK)
                .header(
                        HttpHeaders.SET_COOKIE,
                        createRefreshTokenCookie(tokenResponse.getRefreshToken())
                                .toString())
                .body(tokenResponse);
    }

    @PostMapping("/signup")
    public ResponseEntity<MyInfoResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signUp(request));
    }

    @PostMapping("/verify-school")
    public ResponseEntity<MyInfoResponse> verifySchool(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @Valid @RequestBody VerifySchoolRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(authService.verifySchool(userDetails.getUser(), request));
    }

    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofMillis(JwtTokenProvider.REFRESH_TOKEN_VALID_TIME))
                .sameSite("Lax")
                .build();
    }
}
