package com.inuteamflow.server.global.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.domain.user.enums.Role;
import com.inuteamflow.server.domain.user.service.UserDetailsServiceImpl;
import com.inuteamflow.server.global.jwt.refresh.RefreshTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * {@link JwtTokenProvider}의 토큰 발급 결과를 Mockito 기반 단위 테스트로 검증한다.
 * - Spring Context와 실제 데이터베이스를 사용하지 않는다.
 * - 발급된 토큰을 동일한 서명 키로 되파싱해 iat/exp를 직접 확인한다.
 * - 만료 기간은 JwtTokenProvider의 상수를 재사용하지 않고 Duration으로 따로 표현한다.
 *   상수를 그대로 참조하면 시간 단위를 잘못 바꿔도 테스트가 함께 따라가 회귀를 잡지 못한다.
 */
@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    // HMAC-SHA256 서명 키는 최소 32바이트가 필요하다. application-test.yml과 동일한 값을 사용한다.
    private static final String SECRET = "test-jwt-secret-key-for-context-loads-123456";

    @Mock
    UserDetailsServiceImpl userDetailsService;

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Mock
    User user;

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, userDetailsService, refreshTokenRepository);

        when(user.getUserId()).thenReturn(1L);
        when(user.getUsername()).thenReturn("tester");
        when(user.getRole()).thenReturn(Role.USER);

        // 저장된 리프레시 토큰이 없어 신규 저장 분기를 타도록 한다.
        when(refreshTokenRepository.findByUserId(1L)).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("액세스 토큰은 발급 시각으로부터 3시간 뒤 만료된다")
    void generateToken_setsAccessTokenExpiryToThreeHours() {
        TokenResponse response = jwtTokenProvider.generateToken(authentication());

        Claims claims = parseClaims(response.getAccessToken());

        assertThat(issuedToExpiry(claims)).isEqualTo(Duration.ofHours(3));
    }

    @Test
    @DisplayName("리프레시 토큰은 발급 시각으로부터 3일 뒤 만료된다")
    void generateToken_setsRefreshTokenExpiryToThreeDays() {
        TokenResponse response = jwtTokenProvider.generateToken(authentication());

        Claims claims = parseClaims(response.getRefreshToken());

        assertThat(issuedToExpiry(claims)).isEqualTo(Duration.ofDays(3));
    }

    @Test
    @DisplayName("방금 발급한 액세스 토큰은 검증을 통과한다")
    void validateToken_acceptsGeneratedAccessToken() {
        TokenResponse response = jwtTokenProvider.generateToken(authentication());

        assertThat(jwtTokenProvider.validateToken(response.getAccessToken())).isTrue();
    }

    private Authentication authentication() {
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    private static Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private static Duration issuedToExpiry(Claims claims) {
        return Duration.between(
                claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant());
    }
}
