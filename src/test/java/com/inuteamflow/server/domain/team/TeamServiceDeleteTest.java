package com.inuteamflow.server.domain.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.inuteamflow.server.domain.event.entity.Event;
import com.inuteamflow.server.domain.event.entity.EventParticipant;
import com.inuteamflow.server.domain.event.entity.RecurrenceException;
import com.inuteamflow.server.domain.event.entity.RecurrenceExceptionParticipant;
import com.inuteamflow.server.domain.event.entity.RecurrenceRule;
import com.inuteamflow.server.domain.event.enums.EventColor;
import com.inuteamflow.server.domain.event.enums.EventRole;
import com.inuteamflow.server.domain.event.enums.RecurrenceExceptionType;
import com.inuteamflow.server.domain.event.enums.RecurrenceFrequency;
import com.inuteamflow.server.domain.event.repository.EventParticipantRepository;
import com.inuteamflow.server.domain.event.repository.EventRepository;
import com.inuteamflow.server.domain.event.repository.RecurrenceExceptionParticipantRepository;
import com.inuteamflow.server.domain.event.repository.RecurrenceExceptionRepository;
import com.inuteamflow.server.domain.event.repository.RecurrenceRuleRepository;
import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.domain.team.enums.TeamRole;
import com.inuteamflow.server.domain.team.repository.TeamMemberRepository;
import com.inuteamflow.server.domain.team.repository.TeamRepository;
import com.inuteamflow.server.domain.team.service.TeamService;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.domain.user.enums.Department;
import com.inuteamflow.server.domain.user.enums.Role;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.domain.vote.entity.Vote;
import com.inuteamflow.server.domain.vote.entity.VoteAvailability;
import com.inuteamflow.server.domain.vote.entity.VoteDate;
import com.inuteamflow.server.domain.vote.entity.VoteParticipant;
import com.inuteamflow.server.domain.vote.entity.VoteResult;
import com.inuteamflow.server.domain.vote.entity.VoteTimeSlot;
import com.inuteamflow.server.domain.vote.repository.VoteAvailabilityRepository;
import com.inuteamflow.server.domain.vote.repository.VoteDateRepository;
import com.inuteamflow.server.domain.vote.repository.VoteParticipantRepository;
import com.inuteamflow.server.domain.vote.repository.VoteRepository;
import com.inuteamflow.server.domain.vote.repository.VoteResultRepository;
import com.inuteamflow.server.domain.vote.repository.VoteTimeSlotRepository;
import com.inuteamflow.server.global.enums.Category;
import com.inuteamflow.server.global.s3.S3Service;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
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

/**
 * {@link TeamService#deleteTeam}이 팀에 연관된 일정·투표 데이터와 함께 팀을 삭제하는지 검증한다.
 * - 픽스처 생성, 팀 삭제, 결과 조회를 각각 독립된 트랜잭션으로 실행해 실제 커밋 시점의 FK 제약까지 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TeamServiceDeleteTest {

    @Autowired
    private TeamService teamService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private S3Service s3Service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventParticipantRepository eventParticipantRepository;

    @Autowired
    private RecurrenceRuleRepository recurrenceRuleRepository;

    @Autowired
    private RecurrenceExceptionRepository recurrenceExceptionRepository;

    @Autowired
    private RecurrenceExceptionParticipantRepository recurrenceExceptionParticipantRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private VoteDateRepository voteDateRepository;

    @Autowired
    private VoteTimeSlotRepository voteTimeSlotRepository;

    @Autowired
    private VoteParticipantRepository voteParticipantRepository;

    @Autowired
    private VoteAvailabilityRepository voteAvailabilityRepository;

    @Autowired
    private VoteResultRepository voteResultRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("일정·투표가 있는 팀을 삭제하면 연관 데이터와 함께 팀이 삭제된다")
    void deleteTeam_withEventsAndVotes() {
        FixtureIds ids = transactionTemplate.execute(status -> createFixture());
        assertThat(ids).isNotNull();

        User leader = transactionTemplate.execute(
                status -> userRepository.findById(ids.leaderId()).orElseThrow());
        assertThat(leader).isNotNull();

        // 테스트 트랜잭션이 없는 상태에서 호출하므로 메서드 반환 전에 서비스 트랜잭션이 커밋된다.
        assertThatNoException().isThrownBy(() -> teamService.deleteTeam(leader, ids.teamId()));

        transactionTemplate.executeWithoutResult(status -> {
            assertThat(teamRepository.findById(ids.teamId())).isEmpty();
            assertThat(teamMemberRepository.findById(ids.leaderMemberId())).isEmpty();
            assertThat(teamMemberRepository.findById(ids.memberMemberId())).isEmpty();

            assertThat(eventRepository.findById(ids.singleEventId())).isEmpty();
            assertThat(eventRepository.findById(ids.recurringEventId())).isEmpty();
            assertThat(eventParticipantRepository.findById(ids.hostParticipantId()))
                    .isEmpty();
            assertThat(eventParticipantRepository.findById(ids.memberParticipantId()))
                    .isEmpty();
            assertThat(recurrenceRuleRepository.findById(ids.recurrenceRuleId()))
                    .isEmpty();
            assertThat(recurrenceExceptionRepository.findById(ids.recurrenceExceptionId()))
                    .isEmpty();
            assertThat(recurrenceExceptionParticipantRepository.findById(ids.exceptionParticipantId()))
                    .isEmpty();

            assertThat(voteRepository.findById(ids.voteId())).isEmpty();
            assertThat(voteDateRepository.findById(ids.voteDateId())).isEmpty();
            assertThat(voteTimeSlotRepository.findById(ids.voteTimeSlotId())).isEmpty();
            assertThat(voteParticipantRepository.findById(ids.voteParticipantId()))
                    .isEmpty();
            assertThat(voteAvailabilityRepository.findById(ids.voteAvailabilityId()))
                    .isEmpty();
            assertThat(voteResultRepository.findById(ids.voteResultId())).isEmpty();

            // 팀과 무관한 사용자 계정은 유지된다.
            assertThat(userRepository.findById(ids.leaderId())).isPresent();
            assertThat(userRepository.findById(ids.memberId())).isPresent();
        });
    }

    private FixtureIds createFixture() {
        User leader = createUser("delete-team-leader", "delete-team-leader@inu.ac.kr");
        User member = createUser("delete-team-member", "delete-team-member@inu.ac.kr");

        actingAs(leader);
        Team team = teamRepository.save(Team.builder()
                .name("delete-team")
                .description("desc")
                .category(Category.PROJECT)
                .build());
        TeamMember leaderMember = teamMemberRepository.save(TeamMember.create(team, leader, TeamRole.LEADER));
        TeamMember memberMember = teamMemberRepository.save(TeamMember.create(team, member, TeamRole.MEMBER));

        // 단일 팀 일정 + 참여자
        LocalDateTime start = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(10, 0));
        Event singleEvent = eventRepository.save(teamEvent(team, "single-event", start, true));
        EventParticipant hostParticipant =
                eventParticipantRepository.save(EventParticipant.create(singleEvent, leaderMember, EventRole.HOST));
        EventParticipant memberParticipant = eventParticipantRepository.save(
                EventParticipant.create(singleEvent, memberMember, EventRole.PARTICIPANT));

        // 반복 팀 일정 + 반복 규칙(요일 목록) + 수정된 회차 + 회차 참여자
        Event recurringEvent = eventRepository.save(teamEvent(team, "recurring-event", start, false));
        eventParticipantRepository.save(EventParticipant.create(recurringEvent, leaderMember, EventRole.HOST));
        RecurrenceRule recurrenceRule = recurrenceRuleRepository.save(RecurrenceRule.builder()
                .event(recurringEvent)
                .freq(RecurrenceFrequency.WEEKLY)
                .intervalValue(1)
                .byDay(List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
                .seriesStartAt(start)
                .build());
        RecurrenceException recurrenceException = recurrenceExceptionRepository.save(RecurrenceException.builder()
                .event(recurringEvent)
                .originalOccurrenceAt(start.plusWeeks(1))
                .exceptionType(RecurrenceExceptionType.MODIFIED)
                .modifiedTitle("modified")
                .modifiedStartAt(start.plusWeeks(1))
                .modifiedEndAt(start.plusWeeks(1).plusHours(1))
                .modifiedAllDay(false)
                .modifiedIsFinished(false)
                .build());
        RecurrenceExceptionParticipant exceptionParticipant = recurrenceExceptionParticipantRepository.save(
                RecurrenceExceptionParticipant.create(recurrenceException, memberMember, EventRole.PARTICIPANT));

        // 팀 투표 + 날짜/시간 슬롯 + 참여자/가능 시간 + 확정 결과(일정 연결)
        Vote vote = voteRepository.save(Vote.builder()
                .team(team)
                .title("delete-team-vote")
                .description("desc")
                .isOpened(false)
                .isAllDay(false)
                .dailyTimeStart(LocalTime.of(9, 0))
                .dailyTimeEnd(LocalTime.of(20, 0))
                .slotUnitMinute(30)
                .build());
        VoteDate voteDate =
                voteDateRepository.save(VoteDate.create(vote, LocalDate.now().plusDays(1)));
        VoteTimeSlot voteTimeSlot =
                voteTimeSlotRepository.save(VoteTimeSlot.create(voteDate, LocalTime.of(9, 0), LocalTime.of(9, 30)));
        VoteParticipant voteParticipant = voteParticipantRepository.save(VoteParticipant.create(vote, memberMember));
        VoteAvailability voteAvailability =
                voteAvailabilityRepository.save(VoteAvailability.create(voteParticipant, voteTimeSlot));
        VoteResult voteResult =
                voteResultRepository.save(VoteResult.create(vote, singleEvent, false, start, start.plusHours(1)));

        return new FixtureIds(
                leader.getUserId(),
                member.getUserId(),
                team.getTeamId(),
                leaderMember.getTeamMemberId(),
                memberMember.getTeamMemberId(),
                singleEvent.getEventId(),
                recurringEvent.getEventId(),
                hostParticipant.getEventParticipantId(),
                memberParticipant.getEventParticipantId(),
                recurrenceRule.getRecurrenceRuleId(),
                recurrenceException.getRecurrenceExceptionId(),
                exceptionParticipant.getRecurrenceExceptionParticipantId(),
                vote.getVoteId(),
                voteDate.getVoteDateId(),
                voteTimeSlot.getVoteTimeSlotId(),
                voteParticipant.getVoteParticipantId(),
                voteAvailability.getVoteAvailabilityId(),
                voteResult.getVoteResultId());
    }

    private Event teamEvent(Team team, String title, LocalDateTime start, boolean isSingle) {
        return Event.builder()
                .team(team)
                .title(title)
                .description("desc")
                .startAt(start)
                .endAt(start.plusHours(1))
                .isAllDay(false)
                .color(EventColor.OCEAN)
                .uid(UUID.randomUUID().toString())
                .sequence(0)
                .isFinished(false)
                .isSingle(isSingle)
                .build();
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
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    private record FixtureIds(
            Long leaderId,
            Long memberId,
            Long teamId,
            Long leaderMemberId,
            Long memberMemberId,
            Long singleEventId,
            Long recurringEventId,
            Long hostParticipantId,
            Long memberParticipantId,
            Long recurrenceRuleId,
            Long recurrenceExceptionId,
            Long exceptionParticipantId,
            Long voteId,
            Long voteDateId,
            Long voteTimeSlotId,
            Long voteParticipantId,
            Long voteAvailabilityId,
            Long voteResultId) {}
}
