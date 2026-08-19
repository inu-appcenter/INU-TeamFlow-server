package com.inuteamflow.server.domain.vote;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.inuteamflow.server.domain.vote.entity.Vote;
import com.inuteamflow.server.domain.vote.repository.VoteRepository;
import com.inuteamflow.server.domain.vote.service.VoteService;
import com.inuteamflow.server.global.enums.Category;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import java.time.LocalTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link VoteService}의 투표 조회 API에서 팀 멤버십 검증이 누락된 접근제어 취약점(이슈 #94 계열)을 실제 Repository와 H2
 * 데이터베이스로 검증한다.
 *
 * <p>쓰기/액션 계열은 {@code validateTeamMember(...)}로 멤버십을 검증하지만, 조회 경로에는 누락되어 팀에 속하지 않은
 * 사용자가 투표 ID를 열거해 참가자 이름·투표 여부·팀 일정을 조회할 수 있다.</p>
 *
 * <ul>
 *   <li>{@code getVotes}, {@code getVote}, {@code getTimeSlot}: 비멤버 요청이 거부되어야 함을 검증한다.</li>
 * </ul>
 *
 * <p>담당 개발자의 프로덕션 수정 대기 상태이며, 수정 전에는 테스트가 실패(red)한다.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VoteAuthorizationTest {

    @Autowired
    private VoteService voteService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private VoteRepository voteRepository;

    private User outsider;
    private Team team;
    private Vote vote;

    @BeforeEach
    void setUp() {
        User member = createUser("vote-team-member");
        outsider = createUser("vote-outsider");

        // 투표가 존재하는 팀과, 그 팀에 속하지 않은 외부 사용자(outsider)
        actingAs(member);
        team = teamRepository.save(Team.builder()
                .name("테스트 팀")
                .description("테스트 팀 설명")
                .category(Category.PROJECT)
                .build());
        teamMemberRepository.save(TeamMember.create(team, member, TeamRole.MEMBER));

        vote = voteRepository.save(Vote.builder()
                .team(team)
                .title("팀 일정 투표")
                .description("후보 시간 투표")
                .isOpened(true)
                .isAllDay(false)
                .dailyTimeStart(LocalTime.of(10, 0))
                .dailyTimeEnd(LocalTime.of(11, 0))
                .slotUnitMinute(30)
                .build());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("팀에 속하지 않은 사용자는 팀의 투표 목록을 조회할 수 없다")
    void getVotes_byNonTeamMember_isRejected() {
        actingAs(outsider);
        assertThatThrownBy(() -> voteService.getVotes(outsider, team.getTeamId()))
                .isInstanceOf(RestApiException.class)
                .extracting(exception -> ((RestApiException) exception).getErrorCode())
                .isEqualTo(CustomErrorCode.TEAM_MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("팀에 속하지 않은 사용자는 투표 단건을 조회할 수 없다")
    void getVote_byNonTeamMember_isRejected() {
        actingAs(outsider);
        assertThatThrownBy(() -> voteService.getVote(outsider, vote.getVoteId()))
                .isInstanceOf(RestApiException.class)
                .extracting(exception -> ((RestApiException) exception).getErrorCode())
                .isEqualTo(CustomErrorCode.TEAM_MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("팀에 속하지 않은 사용자는 투표 시간 슬롯을 조회할 수 없다")
    void getTimeSlot_byNonTeamMember_isRejected() {
        actingAs(outsider);
        assertThatThrownBy(() -> voteService.getTimeSlot(outsider, vote.getVoteId()))
                .isInstanceOf(RestApiException.class)
                .extracting(exception -> ((RestApiException) exception).getErrorCode())
                .isEqualTo(CustomErrorCode.TEAM_MEMBER_NOT_FOUND);
    }

    private User createUser(String username) {
        return userRepository.save(User.builder()
                .username(username)
                .email(username + "@inu.ac.kr")
                .password("encoded-password")
                .name(username)
                .department(Department.COMPUTER_SCIENCE)
                .isSchoolVerified(false)
                .role(Role.USER)
                .build());
    }

    private void actingAs(User user) {
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }
}
