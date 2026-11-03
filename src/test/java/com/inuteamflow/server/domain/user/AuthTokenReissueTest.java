package com.inuteamflow.server.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.enums.Department;
import com.inuteamflow.server.domain.user.enums.Role;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.global.enums.ClientType;
import com.inuteamflow.server.global.jwt.refresh.RefreshToken;
import com.inuteamflow.server.global.jwt.refresh.RefreshTokenRepository;
import com.inuteamflow.server.global.s3.S3Service;
import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인/재발급 시 리프레시 토큰의 실패 응답과 클라이언트 타입별 유효기간을 MockMvc로 검증한다.
 * - 만료 기간은 ClientType의 값을 재사용하지 않고 Duration과 초 단위 Max-Age로 따로 표현한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthTokenReissueTest {

    private static final String USERNAME = "reissue-user";
    private static final String PASSWORD = "password123!";
    private static final String WEB_MAX_AGE = "Max-Age=259200"; // 3일
    private static final String MOBILE_MAX_AGE = "Max-Age=2592000"; // 30일

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @MockitoBean
    private S3Service s3Service;

    @Value("${jwt.secret}")
    private String secret;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .username(USERNAME)
                .email("reissue-user@inu.ac.kr")
                .password(bCryptPasswordEncoder.encode(PASSWORD))
                .name("reissue-user")
                .department(Department.COMPUTER_SCIENCE)
                .studentNumber(null)
                .isSchoolVerified(false)
                .role(Role.USER)
                .imageKey(null)
                .build());
    }

    @Test
    @DisplayName("로그아웃한 리프레시 토큰으로 재발급을 요청하면 401을 반환한다")
    void reissue_withLoggedOutRefreshToken_returnsUnauthorized() throws Exception {
        // given
        String refreshToken = refreshTokenOf(login(null));
        mockMvc.perform(post("/api/v1/auth/logout").cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isNoContent());

        // when & then
        reissue(refreshToken)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("리프레시 토큰을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("만료된 리프레시 토큰은 DB에 남아 있어도 재발급하지 않고 401을 반환한다")
    void reissue_withExpiredRefreshToken_returnsUnauthorized() throws Exception {
        // given: 만료됐지만 아직 삭제되지 않은 토큰
        Instant issuedAt = Instant.now().minus(Duration.ofDays(4));
        String expiredToken = refreshToken(issuedAt, issuedAt.plus(Duration.ofDays(3)));
        saveRefreshToken(ClientType.WEB, expiredToken);

        // when & then
        reissue(expiredToken)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("만료된 토큰입니다."));
    }

    @Test
    @DisplayName("X-Client-Type 헤더 없이 로그인하면 리프레시 토큰 유효기간은 3일이다")
    void login_withoutClientTypeHeader_issuesWebRefreshToken() throws Exception {
        // when
        ResultActions result = login(null)
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(WEB_MAX_AGE)));

        // then
        assertThat(validTimeOf(refreshTokenOf(result))).isEqualTo(Duration.ofDays(3));
        assertThat(refreshTokenRepository.findByUserIdAndClientType(user.getUserId(), ClientType.WEB))
                .isPresent();
    }

    @Test
    @DisplayName("X-Client-Type: mobile로 로그인하면 리프레시 토큰 유효기간은 30일이다")
    void login_withMobileClientTypeHeader_issuesMobileRefreshToken() throws Exception {
        // when
        ResultActions result = login("mobile")
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(MOBILE_MAX_AGE)))
                .andExpect(jsonPath("$.clientType").doesNotExist());

        // then
        assertThat(validTimeOf(refreshTokenOf(result))).isEqualTo(Duration.ofDays(30));
        assertThat(refreshTokenRepository.findByUserIdAndClientType(user.getUserId(), ClientType.MOBILE))
                .isPresent();
    }

    @Test
    @DisplayName("모바일 리프레시 토큰을 재발급하면 헤더가 없어도 재발급 시점부터 다시 30일이 시작된다")
    void reissue_withMobileRefreshToken_restartsThirtyDays() throws Exception {
        // given: 10일 전에 발급되어 20일 남은 모바일 토큰
        Instant issuedAt = Instant.now().minus(Duration.ofDays(10));
        Instant expiresAt = issuedAt.plus(Duration.ofDays(30));
        String oldToken = refreshToken(issuedAt, expiresAt);
        saveRefreshToken(ClientType.MOBILE, oldToken);

        // when
        ResultActions result = reissue(oldToken)
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(MOBILE_MAX_AGE)));

        // then
        String newToken = refreshTokenOf(result);
        Claims claims = parseClaims(newToken);
        assertThat(validTimeOf(newToken)).isEqualTo(Duration.ofDays(30));
        assertThat(claims.getExpiration().toInstant()).isAfter(expiresAt);
        assertThat(refreshTokenRepository.findByUserIdAndClientType(user.getUserId(), ClientType.MOBILE))
                .get()
                .extracting(RefreshToken::getRefreshToken)
                .isEqualTo(newToken);
    }

    @Test
    @DisplayName("모바일 로그인 후 웹에서 로그인해도 모바일 리프레시 토큰으로 재발급할 수 있다")
    void reissue_withMobileRefreshTokenAfterWebLogin_succeeds() throws Exception {
        // given
        String mobileToken = refreshTokenOf(login("mobile"));
        login(null).andExpect(status().isOk());

        // when & then
        reissue(mobileToken)
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(MOBILE_MAX_AGE)));
        assertThat(refreshTokenRepository.findByUserIdAndClientType(user.getUserId(), ClientType.WEB))
                .isPresent();
    }

    private ResultActions login(String clientType) throws Exception {
        var request = post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + USERNAME + "\",\"password\":\"" + PASSWORD + "\"}");
        if (clientType != null) {
            request.header(ClientType.HEADER_NAME, clientType);
        }
        return mockMvc.perform(request);
    }

    private ResultActions reissue(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/reissue").cookie(new Cookie("refreshToken", refreshToken)));
    }

    private void saveRefreshToken(ClientType clientType, String refreshToken) {
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .clientType(clientType)
                .refreshToken(refreshToken)
                .build());
    }

    private String refreshToken(Instant issuedAt, Instant expiresAt) {
        return Jwts.builder()
                .setSubject(USERNAME)
                .setIssuedAt(Date.from(issuedAt))
                .setExpiration(Date.from(expiresAt))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private static String refreshTokenOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.refreshToken");
    }

    private Duration validTimeOf(String token) {
        Claims claims = parseClaims(token);
        return Duration.between(
                claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant());
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
