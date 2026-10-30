package com.inuteamflow.server.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.enums.Department;
import com.inuteamflow.server.domain.user.enums.Role;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.global.jwt.refresh.RefreshToken;
import com.inuteamflow.server.global.jwt.refresh.RefreshTokenRepository;
import com.inuteamflow.server.global.s3.S3Service;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그아웃 시 리프레시 토큰 무효화와 쿠키 삭제 응답을 MockMvc로 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthLogoutTest {

    private static final String REFRESH_TOKEN = "saved-refresh-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.builder()
                .username("logout-user")
                .email("logout-user@inu.ac.kr")
                .password("encoded-password")
                .name("logout-user")
                .department(Department.COMPUTER_SCIENCE)
                .studentNumber(null)
                .isSchoolVerified(false)
                .role(Role.USER)
                .imageKey(null)
                .build());
        refreshTokenRepository.save(
                RefreshToken.builder().user(user).refreshToken(REFRESH_TOKEN).build());
    }

    @Test
    @DisplayName("로그아웃하면 리프레시 토큰을 삭제하고 Max-Age=0인 Secure 쿠키를 내려준다")
    void logout_withRefreshTokenCookie_deletesTokenAndExpiresCookie() throws Exception {
        // when & then: 웹은 브라우저가 쿠키로 리프레시 토큰을 보낸다
        mockMvc.perform(post("/api/v1/auth/logout").cookie(new Cookie("refreshToken", REFRESH_TOKEN)))
                .andExpect(status().isNoContent())
                .andExpect(header().string(
                                HttpHeaders.SET_COOKIE,
                                allOf(
                                        containsString("refreshToken="),
                                        containsString("Max-Age=0"),
                                        containsString("Secure"),
                                        containsString("HttpOnly"))));

        assertThat(refreshTokenRepository.findByRefreshToken(REFRESH_TOKEN)).isEmpty();
    }

    @Test
    @DisplayName("리프레시 토큰 쿠키가 없어도 로그아웃은 성공하고 쿠키를 만료시킨다")
    void logout_withoutRefreshToken_returnsNoContent() throws Exception {
        // when & then: 쿠키가 없더라도 클라이언트 상태를 정리할 수 있도록 성공 처리한다
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        assertThat(refreshTokenRepository.findByRefreshToken(REFRESH_TOKEN)).isPresent();
    }
}
