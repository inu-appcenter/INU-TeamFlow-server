package com.inuteamflow.server.domain.user;

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
import com.inuteamflow.server.domain.user.service.UserService;
import com.inuteamflow.server.domain.vote.entity.Vote;
import com.inuteamflow.server.domain.vote.entity.VoteAvailability;
import com.inuteamflow.server.domain.vote.entity.VoteDate;
import com.inuteamflow.server.domain.vote.entity.VoteParticipant;
import com.inuteamflow.server.domain.vote.entity.VoteTimeSlot;
import com.inuteamflow.server.domain.vote.repository.VoteAvailabilityRepository;
import com.inuteamflow.server.domain.vote.repository.VoteDateRepository;
import com.inuteamflow.server.domain.vote.repository.VoteParticipantRepository;
import com.inuteamflow.server.domain.vote.repository.VoteRepository;
import com.inuteamflow.server.domain.vote.repository.VoteTimeSlotRepository;
import com.inuteamflow.server.global.enums.Category;
import com.inuteamflow.server.global.s3.S3Service;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * {@link UserService#deleteUser(User)}의 실제 트랜잭션 커밋 경계를 검증한다.
 * - 픽스처 생성, 회원 탈퇴, 결과 조회를 각각 독립된 트랜잭션으로 실행한다.
 * - 서비스 메서드 반환 시 삭제가 실제로 커밋되었는지 새로운 트랜잭션에서 확인한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserServiceCommitTest {

    @Autowired private UserService userService;
    @Autowired private TransactionTemplate transactionTemplate;

    @MockitoBean private S3Service s3Service;

    @Autowired private UserRepository userRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private TeamMemberRepository teamMemberRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private VoteParticipantRepository voteParticipantRepository;
    @Autowired private VoteAvailabilityRepository voteAvailabilityRepository;
    @Autowired private VoteDateRepository voteDateRepository;
    @Autowired private VoteTimeSlotRepository voteTimeSlotRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("계정과 종료 투표의 삭제가 실제 트랜잭션 커밋 후에도 유지된다")
    void deleteUser_commitsAccountAndClosedVoteDeletion() {
        FixtureIds ids = transactionTemplate.execute(status -> createFixture());
        assertThat(ids).isNotNull();

        User target = transactionTemplate.execute(status ->
                userRepository.findById(ids.targetId()).orElseThrow());
        assertThat(target).isNotNull();

        // 테스트 트랜잭션이 없는 상태에서 호출하므로 메서드 반환 전에 서비스 트랜잭션이 커밋된다.
        assertThatNoException().isThrownBy(() -> userService.deleteUser(target));

        transactionTemplate.executeWithoutResult(status -> {
            assertThat(userRepository.findById(ids.targetId())).isEmpty();
            assertThat(teamMemberRepository.findById(ids.targetMemberId())).isEmpty();

            assertThat(voteAvailabilityRepository.findById(ids.targetAvailabilityId())).isEmpty();
            assertThat(voteAvailabilityRepository.findById(ids.leaderAvailabilityId())).isEmpty();
            assertThat(voteParticipantRepository.findById(ids.targetParticipantId())).isEmpty();
            assertThat(voteParticipantRepository.findById(ids.leaderParticipantId())).isEmpty();
            assertThat(voteTimeSlotRepository.findById(ids.voteTimeSlotId())).isEmpty();
            assertThat(voteDateRepository.findById(ids.voteDateId())).isEmpty();
            assertThat(voteRepository.findById(ids.voteId())).isEmpty();

            assertThat(userRepository.findById(ids.leaderId())).isPresent();
            assertThat(teamRepository.findById(ids.teamId())).isPresent();
            assertThat(teamMemberRepository.findById(ids.leaderMemberId())).isPresent();
        });
    }

    private FixtureIds createFixture() {
        User target = createUser("commit-target", "commit-target@inu.ac.kr");
        User leader = createUser("commit-leader", "commit-leader@inu.ac.kr");

        actingAs(leader);
        Team team = teamRepository.save(
                Team.builder()
                        .name("commit-team")
                        .description("desc")
                        .category(Category.PROJECT)
                        .build()
        );
        TeamMember leaderMember = teamMemberRepository.save(
                TeamMember.create(team, leader, TeamRole.LEADER)
        );

        actingAs(target);
        TeamMember targetMember = teamMemberRepository.save(
                TeamMember.create(team, target, TeamRole.MEMBER)
        );
        Vote closedVote = voteRepository.save(
                Vote.builder()
                        .team(team)
                        .title("commit-closed-vote")
                        .description("desc")
                        .isOpened(false)
                        .isAllDay(false)
                        .dailyTimeStart(LocalTime.of(9, 0))
                        .dailyTimeEnd(LocalTime.of(20, 0))
                        .slotUnitMinute(30)
                        .build()
        );
        VoteDate voteDate = voteDateRepository.save(
                VoteDate.create(closedVote, LocalDate.now().plusDays(1))
        );
        VoteTimeSlot voteTimeSlot = voteTimeSlotRepository.save(
                VoteTimeSlot.create(voteDate, LocalTime.of(9, 0), LocalTime.of(9, 30))
        );

        VoteParticipant targetParticipant = voteParticipantRepository.save(
                VoteParticipant.create(closedVote, targetMember)
        );
        VoteParticipant leaderParticipant = voteParticipantRepository.save(
                VoteParticipant.create(closedVote, leaderMember)
        );
        VoteAvailability targetAvailability = voteAvailabilityRepository.save(
                VoteAvailability.create(targetParticipant, voteTimeSlot)
        );
        VoteAvailability leaderAvailability = voteAvailabilityRepository.save(
                VoteAvailability.create(leaderParticipant, voteTimeSlot)
        );

        return new FixtureIds(
                target.getUserId(),
                leader.getUserId(),
                team.getTeamId(),
                targetMember.getTeamMemberId(),
                leaderMember.getTeamMemberId(),
                closedVote.getVoteId(),
                voteDate.getVoteDateId(),
                voteTimeSlot.getVoteTimeSlotId(),
                targetParticipant.getVoteParticipantId(),
                leaderParticipant.getVoteParticipantId(),
                targetAvailability.getVoteAvailabilityId(),
                leaderAvailability.getVoteAvailabilityId()
        );
    }

    private User createUser(String username, String email) {
        return userRepository.save(
                User.builder()
                        .username(username)
                        .email(email)
                        .password("encoded-password")
                        .name(username)
                        .department(Department.COMPUTER_SCIENCE)
                        .studentNumber(null)
                        .isSchoolVerified(false)
                        .role(Role.USER)
                        .imageKey(null)
                        .build()
        );
    }

    private void actingAs(User user) {
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                )
        );
    }

    private record FixtureIds(
            Long targetId,
            Long leaderId,
            Long teamId,
            Long targetMemberId,
            Long leaderMemberId,
            Long voteId,
            Long voteDateId,
            Long voteTimeSlotId,
            Long targetParticipantId,
            Long leaderParticipantId,
            Long targetAvailabilityId,
            Long leaderAvailabilityId
    ) {
    }
}
