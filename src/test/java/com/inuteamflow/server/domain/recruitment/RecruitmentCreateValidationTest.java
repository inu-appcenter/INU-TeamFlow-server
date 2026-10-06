package com.inuteamflow.server.domain.recruitment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.inuteamflow.server.domain.notification.service.NotificationService;
import com.inuteamflow.server.domain.recruitment.repository.RecruitmentRepository;
import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.domain.team.enums.TeamRole;
import com.inuteamflow.server.domain.team.repository.TeamMemberRepository;
import com.inuteamflow.server.domain.team.repository.TeamRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.domain.user.enums.Department;
import com.inuteamflow.server.domain.user.enums.Role;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.global.enums.Category;
import com.inuteamflow.server.global.s3.S3Service;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 모집글 생성 시 모집 목표 인원(targetMemberCount) 검증을 실제 Repository와 H2 데이터베이스로 검증한다.
 *
 * <p>검증(@Positive)은 컨트롤러의 @Valid 단계에서 수행되므로 MockMvc로 실제 API를 호출한다.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RecruitmentCreateValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private RecruitmentRepository recruitmentRepository;

    @MockitoBean
    private S3Service s3Service;

    @MockitoBean
    private NotificationService notificationService;

    private User teamLeader;
    private Team team;

    @BeforeEach
    void setUp() {
        teamLeader = createUser("team-leader", "team-leader@inu.ac.kr");

        // 모집글을 작성할 권한이 있는 리더가 소유한 팀
        actingAs(teamLeader);
        team = teamRepository.save(Team.builder()
                .name("테스트 팀")
                .description("테스트 팀 설명")
                .category(Category.PROJECT)
                .build());
        teamMemberRepository.save(TeamMember.create(team, teamLeader, TeamRole.LEADER));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("모집 목표 인원이 음수면 400 에러를 반환하고 모집글을 생성하지 않는다")
    void createRecruitment_withNegativeTargetMemberCount_returnsBadRequest() throws Exception {
        // given: 팀 리더가 모집 목표 인원을 음수로 요청
        String body = """
                {
                  "title": "모집글",
                  "category": "PROJECT",
                  "description": "모집글 설명",
                  "teamId": %d,
                  "targetMemberCount": -1,
                  "endAt": "%s"
                }
                """.formatted(team.getTeamId(), LocalDateTime.now().plusDays(7));

        // when & then: 요청 검증 단계에서 거부되어 400(COMMON_INVALID_REQUEST)을 반환해야 한다
        mockMvc.perform(post("/api/v1/recruitments")
                        .with(authentication(authenticationOf(teamLeader)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        assertThat(recruitmentRepository.count()).isZero();
    }

    private User createUser(String username, String email) {
        return userRepository.save(User.builder()
                .username(username)
                .email(email)
                .password("encoded-password")
                .name(username)
                .department(Department.COMPUTER_SCIENCE)
                .studentNumber(null)
                .isSchoolVerified(false)
                .role(Role.USER)
                .imageKey(null)
                .build());
    }

    private void actingAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(authenticationOf(user));
    }

    private UsernamePasswordAuthenticationToken authenticationOf(User user) {
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
