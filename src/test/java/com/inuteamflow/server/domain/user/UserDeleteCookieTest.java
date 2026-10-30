package com.inuteamflow.server.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.domain.user.enums.Department;
import com.inuteamflow.server.domain.user.enums.Role;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.global.jwt.refresh.RefreshToken;
import com.inuteamflow.server.global.jwt.refresh.RefreshTokenRepository;
import com.inuteamflow.server.global.s3.S3Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 탈퇴 시 리프레시 토큰 삭제와 쿠키 삭제 응답을 MockMvc로 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserDeleteCookieTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    private S3Service s3Service;

    @Test
    @DisplayName("회원 탈퇴하면 리프레시 토큰을 삭제하고 Max-Age=0인 쿠키를 내려준다")
    void deleteUser_deletesTokenAndExpiresCookie() throws Exception {
        // given: 리프레시 토큰이 저장된 사용자
        User user = userRepository.save(User.builder()
                .username("delete-user")
                .email("delete-user@inu.ac.kr")
                .password("encoded-password")
                .name("delete-user")
                .department(Department.COMPUTER_SCIENCE)
                .studentNumber(null)
                .isSchoolVerified(false)
                .role(Role.USER)
                .imageKey(null)
                .build());
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .refreshToken("saved-refresh-token")
                .build());
        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        // when & then
        mockMvc.perform(delete("/api/v1/users/me")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()))))
                .andExpect(status().isNoContent())
                .andExpect(header().string(
                                HttpHeaders.SET_COOKIE,
                                allOf(
                                        containsString("refreshToken="),
                                        containsString("Max-Age=0"),
                                        containsString("Secure"),
                                        containsString("HttpOnly"))));

        assertThat(refreshTokenRepository.findByUserId(user.getUserId())).isEmpty();
    }
}
