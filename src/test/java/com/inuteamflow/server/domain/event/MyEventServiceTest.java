package com.inuteamflow.server.domain.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.inuteamflow.server.domain.event.dto.Participant;
import com.inuteamflow.server.domain.event.dto.response.EventListResponse;
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
import com.inuteamflow.server.domain.event.service.MyEventService;
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
import jakarta.persistence.EntityManager;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
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
 * {@link MyEventService#getMyEventList}의 월간 조회 결과를 검증한다.
 * - 단일 일정과 반복 회차의 병합, 취소·수정 예외 반영, 월 경계에 걸친 회차, 참여 일정 필터링을 확인한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MyEventServiceTest {

    private static final int YEAR = 2026;
    private static final int JULY = 7;
    private static final int AUGUST = 8;

    @Autowired
    private MyEventService myEventService;

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
    private EntityManager entityManager;

    private User me;
    private User teammate;
    private Team team;
    private TeamMember myMember;
    private TeamMember teammateMember;

    @BeforeEach
    void setUp() {
        me = saveUser("event-me");
        teammate = saveUser("event-teammate");

        actingAs(me);
        team = teamRepository.saveAndFlush(Team.builder()
                .name("내 일정 조회 테스트 팀")
                .description("참여 일정 조회를 검증하는 팀")
                .category(Category.PROJECT)
                .build());
        myMember = teamMemberRepository.save(TeamMember.create(team, me, TeamRole.MEMBER));
        teammateMember = teamMemberRepository.save(TeamMember.create(team, teammate, TeamRole.LEADER));
        teamMemberRepository.flush();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("단일 일정과 반복 회차를 병합해 시작 시각 오름차순으로 반환한다")
    void getMyEventList_mergesSingleAndRecurring_sortedByStartAt() {
        saveSingleEvent(null, "단일", at(7, 8, 9), at(7, 8, 10));
        Event recurring = saveRecurringEvent(null, "반복", at(7, 7, 12), at(7, 7, 13));
        saveRule(recurring, RecurrenceFrequency.WEEKLY, List.of(DayOfWeek.TUESDAY), null, 2);

        List<EventListResponse> responses = getList(JULY);

        assertThat(responses).extracting(EventListResponse::getTitle).containsExactly("반복", "단일", "반복");
        assertThat(responses)
                .extracting(EventListResponse::getStartAt)
                .containsExactly(at(7, 7, 12), at(7, 8, 9), at(7, 14, 12));
    }

    @Test
    @DisplayName("다른 사용자의 개인 일정은 조회되지 않는다")
    void getMyEventList_excludesOtherUsersPersonalEvents() {
        actingAs(teammate);
        saveSingleEvent(null, "남의 일정", at(7, 8, 9), at(7, 8, 10));
        actingAs(me);

        assertThat(getList(JULY)).isEmpty();
    }

    @Test
    @DisplayName("취소된 회차는 응답에서 제외한다")
    void getMyEventList_excludesCancelledOccurrence() {
        Event recurring = saveRecurringEvent(null, "반복", at(7, 1, 9), at(7, 1, 10));
        saveRule(recurring, RecurrenceFrequency.DAILY, List.of(), null, 3);
        saveCancelled(recurring, at(7, 2, 9));

        assertThat(getList(JULY))
                .extracting(EventListResponse::getOccurrenceAt)
                .containsExactly(at(7, 1, 9), at(7, 3, 9));
    }

    @Test
    @DisplayName("이전 달에 시작해 조회 월까지 이어지는 회차를 포함한다")
    void getMyEventList_includesOccurrenceCrossingMonthBoundary() {
        Event recurring = saveRecurringEvent(null, "월 경계", at(6, 30, 22), at(7, 1, 2));
        saveRule(recurring, RecurrenceFrequency.DAILY, List.of(), null, 1);

        assertThat(getList(JULY)).extracting(EventListResponse::getOccurrenceAt).containsExactly(at(6, 30, 22));
    }

    @Test
    @DisplayName("이전 달에 시작해 조회 월까지 이어지는 회차가 취소되면 제외한다")
    void getMyEventList_excludesCancelledOccurrenceCrossingMonthBoundary() {
        Event recurring = saveRecurringEvent(null, "월 경계", at(6, 30, 22), at(7, 1, 2));
        saveRule(recurring, RecurrenceFrequency.DAILY, List.of(), null, 1);
        saveCancelled(recurring, at(6, 30, 22));

        assertThat(getList(JULY)).isEmpty();
    }

    @Test
    @DisplayName("다른 달의 회차를 조회 월로 옮기면 수정된 시각으로 조회 월에만 노출한다")
    void getMyEventList_includesOccurrenceMovedIntoRange() {
        Event recurring = saveRecurringEvent(null, "반복", at(6, 15, 9), at(6, 15, 10));
        saveRule(recurring, RecurrenceFrequency.WEEKLY, List.of(DayOfWeek.MONDAY), null, 1);
        saveModified(recurring, at(6, 15, 9), "옮긴 회차", at(7, 10, 9), at(7, 10, 10));

        List<EventListResponse> july = getList(JULY);

        assertThat(july).hasSize(1);
        assertThat(july.get(0).getTitle()).isEqualTo("옮긴 회차");
        assertThat(july.get(0).getIsException()).isTrue();
        assertThat(july.get(0).getOccurrenceAt()).isEqualTo(at(6, 15, 9));
        assertThat(july.get(0).getStartAt()).isEqualTo(at(7, 10, 9));
        assertThat(getList(6)).isEmpty();
    }

    @Test
    @DisplayName("조회 월의 회차를 다른 달로 옮기면 조회 월에는 노출하지 않는다")
    void getMyEventList_excludesOccurrenceMovedOutOfRange() {
        Event recurring = saveRecurringEvent(null, "반복", at(7, 1, 9), at(7, 1, 10));
        saveRule(recurring, RecurrenceFrequency.DAILY, List.of(), null, 1);
        saveModified(recurring, at(7, 1, 9), "옮긴 회차", at(8, 5, 9), at(8, 5, 10));

        assertThat(getList(JULY)).isEmpty();
        assertThat(getList(AUGUST)).extracting(EventListResponse::getTitle).containsExactly("옮긴 회차");
    }

    @Test
    @DisplayName("옮겨온 회차의 참여자 정보를 함께 반환한다")
    void getMyEventList_returnsParticipantsOfMovedOccurrence() {
        Event recurring = saveRecurringEvent(team, "팀 반복", at(6, 15, 9), at(6, 15, 10));
        saveRule(recurring, RecurrenceFrequency.DAILY, List.of(), null, 1);
        saveEventParticipant(recurring, myMember);
        RecurrenceException moved = saveModified(recurring, at(6, 15, 9), "옮긴 회차", at(7, 10, 9), at(7, 10, 10));
        saveExceptionParticipant(moved, myMember);
        saveExceptionParticipant(moved, teammateMember);

        List<EventListResponse> responses = getList(JULY);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getIsParticipant()).isTrue();
        assertThat(responses.get(0).getParticipants())
                .extracting(Participant::getTeamMemberId)
                .containsExactlyInAnyOrder(myMember.getTeamMemberId(), teammateMember.getTeamMemberId());
    }

    @Test
    @DisplayName("반복이 종료된 일정은 조회하지 않지만 조회 월로 옮겨진 회차는 노출한다")
    void getMyEventList_endedRule_returnsOnlyMovedOccurrence() {
        Event ended = saveRecurringEvent(null, "종료된 반복", at(6, 1, 9), at(6, 1, 10));
        saveRule(ended, RecurrenceFrequency.DAILY, List.of(), at(6, 20, 0), null);
        Event endedWithMoved = saveRecurringEvent(null, "종료된 반복(이동)", at(6, 1, 9), at(6, 1, 10));
        saveRule(endedWithMoved, RecurrenceFrequency.DAILY, List.of(), at(6, 20, 0), null);
        saveModified(endedWithMoved, at(6, 15, 9), "옮긴 회차", at(7, 10, 9), at(7, 10, 10));

        assertThat(getList(JULY)).extracting(EventListResponse::getTitle).containsExactly("옮긴 회차");
    }

    @Test
    @DisplayName("참여 중인 팀 일정만 팀 정보와 함께 조회한다")
    void getMyEventList_returnsOnlyParticipatingTeamEvents() {
        Event joined = saveSingleEvent(team, "참여 일정", at(7, 2, 9), at(7, 2, 10));
        saveEventParticipant(joined, myMember);
        saveEventParticipant(joined, teammateMember);
        Event notJoined = saveSingleEvent(team, "미참여 일정", at(7, 3, 9), at(7, 3, 10));
        saveEventParticipant(notJoined, teammateMember);

        List<EventListResponse> responses = getList(JULY);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getTitle()).isEqualTo("참여 일정");
        assertThat(responses.get(0).getTeamName()).isEqualTo(team.getName());
        assertThat(responses.get(0).getIsParticipant()).isTrue();
    }

    @Test
    @DisplayName("참여 중인 반복 일정에서 내가 빠진 수정 회차는 제외한다")
    void getMyEventList_participatingRecurring_excludesExceptionWithoutMe() {
        Event recurring = saveRecurringEvent(team, "팀 반복", at(7, 1, 9), at(7, 1, 10));
        saveRule(recurring, RecurrenceFrequency.DAILY, List.of(), null, 3);
        saveEventParticipant(recurring, myMember);
        RecurrenceException withoutMe = saveModified(recurring, at(7, 2, 9), "나 없는 회차", at(7, 2, 11), at(7, 2, 12));
        saveExceptionParticipant(withoutMe, teammateMember);

        assertThat(getList(JULY))
                .extracting(EventListResponse::getOccurrenceAt)
                .containsExactly(at(7, 1, 9), at(7, 3, 9));
    }

    private List<EventListResponse> getList(int month) {
        entityManager.flush();
        entityManager.clear();
        return myEventService.getMyEventList(me, YEAR, month);
    }

    private LocalDateTime at(int month, int day, int hour) {
        return LocalDateTime.of(YEAR, month, day, hour, 0);
    }

    private Event saveSingleEvent(Team eventTeam, String title, LocalDateTime startAt, LocalDateTime endAt) {
        return saveEvent(eventTeam, title, startAt, endAt, true);
    }

    private Event saveRecurringEvent(Team eventTeam, String title, LocalDateTime startAt, LocalDateTime endAt) {
        return saveEvent(eventTeam, title, startAt, endAt, false);
    }

    private Event saveEvent(
            Team eventTeam, String title, LocalDateTime startAt, LocalDateTime endAt, boolean isSingle) {
        return eventRepository.save(Event.builder()
                .team(eventTeam)
                .title(title)
                .startAt(startAt)
                .endAt(endAt)
                .isAllDay(false)
                .color(EventColor.MINT)
                .uid(UUID.randomUUID().toString())
                .sequence(0)
                .isFinished(false)
                .isSingle(isSingle)
                .build());
    }

    private void saveRule(
            Event event,
            RecurrenceFrequency freq,
            List<DayOfWeek> byDay,
            LocalDateTime untilAt,
            Integer occurrenceCount) {
        recurrenceRuleRepository.save(RecurrenceRule.builder()
                .event(event)
                .freq(freq)
                .intervalValue(1)
                .byDay(byDay)
                .seriesStartAt(event.getStartAt())
                .untilAt(untilAt)
                .occurrenceCount(occurrenceCount)
                .build());
    }

    private void saveCancelled(Event event, LocalDateTime originalOccurrenceAt) {
        recurrenceExceptionRepository.save(RecurrenceException.createCancelled(event, originalOccurrenceAt));
    }

    private RecurrenceException saveModified(
            Event event, LocalDateTime originalOccurrenceAt, String title, LocalDateTime startAt, LocalDateTime endAt) {
        return recurrenceExceptionRepository.save(RecurrenceException.builder()
                .event(event)
                .originalOccurrenceAt(originalOccurrenceAt)
                .exceptionType(RecurrenceExceptionType.MODIFIED)
                .modifiedTitle(title)
                .modifiedStartAt(startAt)
                .modifiedEndAt(endAt)
                .modifiedColor(EventColor.MINT)
                .modifiedAllDay(false)
                .modifiedIsFinished(false)
                .build());
    }

    private void saveEventParticipant(Event event, TeamMember member) {
        eventParticipantRepository.save(EventParticipant.create(event, member, EventRole.PARTICIPANT));
    }

    private void saveExceptionParticipant(RecurrenceException exception, TeamMember member) {
        recurrenceExceptionParticipantRepository.save(
                RecurrenceExceptionParticipant.create(exception, member, EventRole.PARTICIPANT));
    }

    private User saveUser(String username) {
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
