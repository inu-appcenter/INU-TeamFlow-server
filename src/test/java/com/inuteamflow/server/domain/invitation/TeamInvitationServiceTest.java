package com.inuteamflow.server.domain.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.inuteamflow.server.domain.chat.service.ChatRoomService;
import com.inuteamflow.server.domain.invitation.dto.response.InvitationCandidateResponse;
import com.inuteamflow.server.domain.invitation.entity.TeamInvitation;
import com.inuteamflow.server.domain.invitation.enums.InvitationCandidateStatus;
import com.inuteamflow.server.domain.invitation.repository.TeamInvitationRepository;
import com.inuteamflow.server.domain.invitation.service.TeamInvitationService;
import com.inuteamflow.server.domain.notification.service.NotificationService;
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
import com.inuteamflow.server.global.enums.Status;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TeamInvitationServiceTest {

    private static final String CANDIDATE_NAME = "초대후보";

    @Autowired
    private TeamInvitationService teamInvitationService;

    @Autowired
    private TeamInvitationRepository teamInvitationRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private ChatRoomService chatRoomService;

    @MockitoBean
    private NotificationService notificationService;

    private User requester;
    private User currentMember;
    private User pendingCandidate;
    private User declinedCandidate;
    private User canceledCandidate;
    private User acceptedFormerMember;
    private User neverInvitedCandidate;
    private User unverifiedCandidate;
    private User outsider;
    private Team team;

    @BeforeEach
    void setUp() {
        requester = saveUser("invitation-requester", "요청자", true);
        currentMember = saveUser("invitation-member", CANDIDATE_NAME + "-팀원", true);
        pendingCandidate = saveUser("invitation-pending", CANDIDATE_NAME + "-대기", true);
        declinedCandidate = saveUser("invitation-declined", CANDIDATE_NAME + "-거절", true);
        canceledCandidate = saveUser("invitation-canceled", CANDIDATE_NAME + "-취소", true);
        acceptedFormerMember = saveUser("invitation-former", CANDIDATE_NAME + "-탈퇴", true);
        neverInvitedCandidate = saveUser("invitation-none", CANDIDATE_NAME + "-미초대", true);
        unverifiedCandidate = saveUser("invitation-unverified", CANDIDATE_NAME + "-미인증", false);
        outsider = saveUser("invitation-outsider", "외부 사용자", true);

        actingAs(requester);
        team = teamRepository.saveAndFlush(Team.builder()
                .name("초대 후보 테스트 팀")
                .description("초대 후보 상태 조회 테스트")
                .category(Category.PROJECT)
                .build());

        // 검색 권한이 리더 역할에 종속되지 않는지 함께 검증하기 위해 일반 팀원으로 구성한다.
        teamMemberRepository.save(TeamMember.create(team, requester, TeamRole.MEMBER));
        teamMemberRepository.save(TeamMember.create(team, currentMember, TeamRole.MEMBER));

        saveInvitation(currentMember, Status.ACCEPTED);
        saveInvitation(pendingCandidate, Status.WAITING);
        saveInvitation(declinedCandidate, Status.DECLINED);
        saveInvitation(canceledCandidate, Status.CANCELED);
        saveInvitation(acceptedFormerMember, Status.ACCEPTED);

        teamMemberRepository.flush();
        teamInvitationRepository.flush();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("현재 팀원과 저장된 초대 상태를 프론트 후보 상태로 변환한다")
    void getCandidates_mapsAllInvitationStates() {
        List<InvitationCandidateResponse> responses =
                teamInvitationService.getCandidates(requester, team.getTeamId(), CANDIDATE_NAME);

        Map<Long, InvitationCandidateStatus> statusByUserId = responses.stream()
                .collect(Collectors.toMap(
                        InvitationCandidateResponse::getUserId, InvitationCandidateResponse::getInvitationStatus));

        assertThat(statusByUserId)
                .containsEntry(currentMember.getUserId(), InvitationCandidateStatus.MEMBER)
                .containsEntry(pendingCandidate.getUserId(), InvitationCandidateStatus.PENDING)
                .containsEntry(declinedCandidate.getUserId(), InvitationCandidateStatus.NONE)
                .containsEntry(canceledCandidate.getUserId(), InvitationCandidateStatus.NONE)
                .containsEntry(acceptedFormerMember.getUserId(), InvitationCandidateStatus.NONE)
                .containsEntry(neverInvitedCandidate.getUserId(), InvitationCandidateStatus.NONE)
                .doesNotContainKey(unverifiedCandidate.getUserId());
    }

    @Test
    @DisplayName("리더가 아닌 일반 팀원도 초대 후보를 검색할 수 있다")
    void getCandidates_allowsRegularTeamMember() {
        List<InvitationCandidateResponse> responses =
                teamInvitationService.getCandidates(requester, team.getTeamId(), CANDIDATE_NAME);

        assertThat(responses).isNotEmpty();
    }

    @Test
    @DisplayName("팀에 속하지 않은 사용자는 초대 후보를 검색할 수 없다")
    void getCandidates_rejectsNonMember() {
        assertThatThrownBy(() -> teamInvitationService.getCandidates(outsider, team.getTeamId(), CANDIDATE_NAME))
                .isInstanceOf(RestApiException.class)
                .extracting(exception -> ((RestApiException) exception).getErrorCode())
                .isEqualTo(CustomErrorCode.TEAM_MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 팀의 초대 후보를 조회하면 예외가 발생한다")
    void getCandidates_rejectsUnknownTeam() {
        assertThatThrownBy(() -> teamInvitationService.getCandidates(requester, Long.MAX_VALUE, CANDIDATE_NAME))
                .isInstanceOf(RestApiException.class)
                .extracting(exception -> ((RestApiException) exception).getErrorCode())
                .isEqualTo(CustomErrorCode.TEAM_NOT_FOUND);
    }

    @Test
    @DisplayName("이름과 일치하는 인증 사용자가 없으면 빈 목록을 반환한다")
    void getCandidates_returnsEmptyListWhenNoUserMatches() {
        List<InvitationCandidateResponse> responses =
                teamInvitationService.getCandidates(requester, team.getTeamId(), "검색결과없음");

        assertThat(responses).isEmpty();
    }

    private User saveUser(String username, String name, boolean schoolVerified) {
        return userRepository.save(User.builder()
                .username(username)
                .email(username + "@inu.ac.kr")
                .password("encoded-password")
                .name(name)
                .department(Department.COMPUTER_SCIENCE)
                .studentNumber(schoolVerified ? username : null)
                .isSchoolVerified(schoolVerified)
                .role(Role.USER)
                .build());
    }

    private TeamInvitation saveInvitation(User receiver, Status status) {
        TeamInvitation invitation = TeamInvitation.create(team, receiver);

        switch (status) {
            case ACCEPTED -> invitation.accept();
            case DECLINED -> invitation.decline();
            case CANCELED -> invitation.cancel();
            case WAITING -> {
                // 생성 상태를 유지한다.
            }
        }

        return teamInvitationRepository.save(invitation);
    }

    private void actingAs(User user) {
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }
}
