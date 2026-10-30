package com.inuteamflow.server.global.jwt.refresh;

import com.inuteamflow.server.global.jwt.JwtTokenProvider;
import java.time.Duration;
import org.springframework.http.ResponseCookie;

public final class RefreshTokenCookie {

    public static final String NAME = "refreshToken";

    private RefreshTokenCookie() {}

    public static ResponseCookie create(String refreshToken) {
        return build(refreshToken, Duration.ofMillis(JwtTokenProvider.REFRESH_TOKEN_VALID_TIME));
    }

    public static ResponseCookie expire() {
        return build("", Duration.ZERO);
    }

    private static ResponseCookie build(String refreshToken, Duration maxAge) {
        return ResponseCookie.from(NAME, refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(maxAge)
                .sameSite("Lax")
                .build();
    }
}
