package com.inuteamflow.server.domain.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inuteamflow.server.domain.event.dto.Participant;
import com.inuteamflow.server.domain.event.dto.request.TeamEventCreateRequest;
import com.inuteamflow.server.domain.event.dto.request.TeamEventUpdateRequest;
import com.inuteamflow.server.domain.event.dto.response.EventDetailResponse;
import com.inuteamflow.server.domain.event.dto.response.EventListResponse;
import com.inuteamflow.server.domain.event.entity.Event;
import com.inuteamflow.server.domain.event.entity.EventParticipant;
import com.inuteamflow.server.domain.event.entity.RecurrenceException;
import com.inuteamflow.server.domain.event.entity.RecurrenceExceptionParticipant;
import com.inuteamflow.server.domain.event.entity.RecurrenceRule;
import com.inuteamflow.server.domain.event.enums.EventColor;
import com.inuteamflow.server.domain.event.enums.EventRole;
import com.inuteamflow.server.domain.event.enums.RecurrenceEditScope;
import com.inuteamflow.server.domain.event.repository.EventParticipantRepository;
import com.inuteamflow.server.domain.event.repository.EventRepository;
import com.inuteamflow.server.domain.event.repository.RecurrenceExceptionParticipantRepository;
import com.inuteamflow.server.domain.event.repository.RecurrenceExceptionRepository;
import com.inuteamflow.server.domain.event.repository.RecurrenceRuleRepository;
import com.inuteamflow.server.domain.event.service.TeamEventService;
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
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
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

/**
 * {@link TeamEventService}의 팀 반복 일정에 대한 동작을 검증한다
 * - 참석자가 정상적으로 포함되어 노출되는지 확인한다.
 * - 반복 일정을 특정 스코프로 변경하였을 때 올바르게 저장되며 응답하는지 확인한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TeamEventServiceTest {

    private static final LocalDateTime SERIES_START_AT = LocalDateTime.of(2026, 7, 6, 10, 0);
    private static final LocalDateTime SERIES_END_AT = LocalDateTime.of(2026, 7, 6, 11, 0);

    @Autowired
    private TeamEventService teamEventService;

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

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationService notificationService;

    private User hostUser;
    private User participantAUser;
    private User participantBUser;
    private User replacementUser;

    private Team team;
    private TeamMember host;
    private TeamMember participantA;
    private TeamMember participantB;
    private TeamMember replacement;

    private Event recurringEvent;
    private RecurrenceRule recurrenceRule;
    private LocalDateTime targetOccurrenceAt;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        hostUser = saveUser("event-host");
        participantAUser = saveUser("participant-a");
        participantBUser = saveUser("participant-b");
        replacementUser = saveUser("participant-replacement");

        actingAs(hostUser);
        team = teamRepository.saveAndFlush(Team.builder()
                .name("반복 일정 참여자 테스트 팀")
                .description("반복 일정 범위별 참여자 변경을 검증하는 팀")
                .category(Category.PROJECT)
                .build());

        host = teamMemberRepository.save(TeamMember.create(team, hostUser, TeamRole.LEADER));
        participantA = teamMemberRepository.save(TeamMember.create(team, participantAUser, TeamRole.MEMBER));
        participantB = teamMemberRepository.save(TeamMember.create(team, participantBUser, TeamRole.MEMBER));
        replacement = teamMemberRepository.save(TeamMember.create(team, replacementUser, TeamRole.MEMBER));
        teamMemberRepository.flush();

        TeamEventCreateRequest createRequest =
                createRecurringEventRequest(List.of(participantA.getTeamMemberId(), participantB.getTeamMemberId()));

        Long eventId = teamEventService
                .createTeamEvent(hostUser, team.getTeamId(), createRequest)
                .getEventId();

        entityManager.flush();
        entityManager.clear();

        recurringEvent = eventRepository.findById(eventId).orElseThrow();
        recurrenceRule = recurrenceRuleRepository.findByEvent(recurringEvent).orElseThrow();

        // 세 번째 발생 일정을 반복 일정 범위별 수정 테스트의 기준으로 사용한다.
        targetOccurrenceAt = SERIES_START_AT.plusDays(2);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void setUp_createsRecurringEventWithHostAndTwoParticipants() {
        List<EventParticipant> participants = eventParticipantRepository.findByEvent(recurringEvent);

        assertThat(recurringEvent.getIsSingle()).isFalse();
        assertThat(recurrenceRule.getSeriesStartAt()).isEqualTo(SERIES_START_AT);
        assertThat(participants).hasSize(3);
        assertThat(participants)
                .filteredOn(participant -> participant.getEventRole() == EventRole.HOST)
                .extracting(EventParticipant::getTeamMemberId)
                .containsExactly(host.getTeamMemberId());
        assertThat(participants)
                .filteredOn(participant -> participant.getEventRole() == EventRole.PARTICIPANT)
                .extracting(EventParticipant::getTeamMemberId)
                .containsExactlyInAnyOrder(participantA.getTeamMemberId(), participantB.getTeamMemberId());
        assertThat(recurrenceExceptionRepository.findByEventIn(List.of(recurringEvent)))
                .isEmpty();
        assertThat(recurrenceExceptionParticipantRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("THIS_INSTANCE로 단일 회차를 수정했을 때, 참석자의 변경이 저장되며 이에 따른 응답이 올바른지 확인한다.")
    void updateThisInstance_savesChangedParticipantsAndReturnsModifiedOccurrence() throws JsonProcessingException {
        TeamEventUpdateRequest request =
                createThisInstanceRequest(List.of(participantB.getTeamMemberId(), replacement.getTeamMemberId()));

        EventDetailResponse response =
                teamEventService.updateTeamEvent(hostUser, team.getTeamId(), recurringEvent.getEventId(), request);

        entityManager.flush();
        entityManager.clear();

        RecurrenceException savedException = recurrenceExceptionRepository
                .findByEventAndOriginalOccurrenceAt(recurringEvent, targetOccurrenceAt)
                .orElseThrow();
        List<RecurrenceExceptionParticipant> savedParticipants =
                recurrenceExceptionParticipantRepository.findByRecurrenceExceptionInWithMember(List.of(savedException));

        assertThat(savedException.getModifiedStartAt()).isEqualTo(targetOccurrenceAt.plusHours(2));
        assertThat(savedException.getModifiedEndAt()).isEqualTo(targetOccurrenceAt.plusHours(3));
        assertThat(savedParticipants)
                .extracting(participant -> participant.getTeamMember().getTeamMemberId())
                .containsExactlyInAnyOrder(
                        host.getTeamMemberId(), participantB.getTeamMemberId(), replacement.getTeamMemberId())
                .doesNotContain(participantA.getTeamMemberId());

        assertThat(response.getEventId()).isEqualTo(recurringEvent.getEventId());
        assertThat(response.getOccurrenceAt()).isEqualTo(targetOccurrenceAt);
        assertThat(response.getStartAt()).isEqualTo(targetOccurrenceAt.plusHours(2));
        assertThat(response.getEndAt()).isEqualTo(targetOccurrenceAt.plusHours(3));
        assertThat(response.getTitle()).isEqualTo("수정된 주간 진행 회의");
        assertThat(response.getColor()).isEqualTo(EventColor.MINT);
        assertThat(response.getIsException()).isTrue();
        assertThat(response.getIsParticipant()).isTrue();
        assertThat(response.getParticipants())
                .extracting(Participant::getTeamMemberId)
                .containsExactlyInAnyOrder(
                        host.getTeamMemberId(), participantB.getTeamMemberId(), replacement.getTeamMemberId());
    }

    @Test
    @DisplayName("THIS_AND_FOLLOWING 으로 이후 회차를 수정했을 때, 참석자의 변경이 저장되며 이에 따른 응답이 올바른지 확인한다.")
    void updateThisAndFollowing_savesChangedParticipantsAndReturnsFollowingSeries() throws JsonProcessingException {
        TeamEventUpdateRequest request =
                createThisAndFollowingRequest(List.of(participantB.getTeamMemberId(), replacement.getTeamMemberId()));

        EventDetailResponse response =
                teamEventService.updateTeamEvent(hostUser, team.getTeamId(), recurringEvent.getEventId(), request);

        entityManager.flush();
        entityManager.clear();

        Event followingEvent = eventRepository.findById(response.getEventId()).orElseThrow();
        RecurrenceRule followingRule =
                recurrenceRuleRepository.findByEvent(followingEvent).orElseThrow();
        RecurrenceRule originalRule =
                recurrenceRuleRepository.findByEvent(recurringEvent).orElseThrow();
        List<EventParticipant> followingParticipants = eventParticipantRepository.findByEvent(followingEvent);

        assertThat(response.getEventId()).isNotEqualTo(recurringEvent.getEventId());
        assertThat(followingEvent.getStartAt()).isEqualTo(targetOccurrenceAt);
        assertThat(followingRule.getSeriesStartAt()).isEqualTo(targetOccurrenceAt);
        assertThat(followingRule.getOccurrenceCount()).isEqualTo(8);
        assertThat(originalRule.getUntilAt()).isEqualTo(targetOccurrenceAt.minusSeconds(1));
        assertThat(originalRule.getOccurrenceCount()).isNull();
        assertThat(followingParticipants)
                .extracting(EventParticipant::getTeamMemberId)
                .containsExactlyInAnyOrder(
                        host.getTeamMemberId(), participantB.getTeamMemberId(), replacement.getTeamMemberId())
                .doesNotContain(participantA.getTeamMemberId());

        assertThat(response.getOccurrenceAt()).isNull();
        assertThat(response.getStartAt()).isEqualTo(targetOccurrenceAt);
        assertThat(response.getEndAt()).isEqualTo(targetOccurrenceAt.plusHours(1));
        assertThat(response.getTitle()).isEqualTo("수정된 주간 진행 회의");
        assertThat(response.getColor()).isEqualTo(EventColor.MINT);
        assertThat(response.getIsException()).isFalse();
        assertThat(response.getIsParticipant()).isTrue();
        assertThat(response.getParticipants())
                .extracting(Participant::getTeamMemberId)
                .containsExactlyInAnyOrder(
                        host.getTeamMemberId(), participantB.getTeamMemberId(), replacement.getTeamMemberId());
    }

    @Test
    @DisplayName("특정 월에 대한 반복 일정 응답이 올바른지 검증한다.")
    void getTeamEventList_returnsAllOccurrencesForRequestedMonth() {
        List<EventListResponse> responses = teamEventService.getTeamEventList(hostUser, team.getTeamId(), 2026, 7);

        assertThat(responses).hasSize(10);
        assertThat(responses)
                .extracting(EventListResponse::getOccurrenceAt)
                .containsExactly(
                        SERIES_START_AT,
                        SERIES_START_AT.plusDays(1),
                        SERIES_START_AT.plusDays(2),
                        SERIES_START_AT.plusDays(3),
                        SERIES_START_AT.plusDays(4),
                        SERIES_START_AT.plusDays(5),
                        SERIES_START_AT.plusDays(6),
                        SERIES_START_AT.plusDays(7),
                        SERIES_START_AT.plusDays(8),
                        SERIES_START_AT.plusDays(9));
        assertThat(responses).allSatisfy(response -> {
            assertThat(response.getEventId()).isEqualTo(recurringEvent.getEventId());
            assertThat(response.getTeamId()).isEqualTo(team.getTeamId());
            assertThat(response.getTeamName()).isEqualTo(team.getName());
            assertThat(response.getTitle()).isEqualTo("주간 진행 회의");
            assertThat(response.getEndAt()).isEqualTo(response.getStartAt().plusHours(1));
            assertThat(response.getIsSingle()).isFalse();
            assertThat(response.getIsException()).isFalse();
            assertThat(response.getIsParticipant()).isTrue();
            assertThat(response.getParticipants())
                    .extracting(Participant::getTeamMemberId)
                    .containsExactlyInAnyOrder(
                            host.getTeamMemberId(), participantA.getTeamMemberId(), participantB.getTeamMemberId());
        });
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

    private TeamEventCreateRequest createRecurringEventRequest(List<Long> participantIds)
            throws JsonProcessingException {
        return objectMapper.readValue(
                """
                {
                  "title": "주간 진행 회의",
                  "description": "반복 일정 참여자 테스트",
                  "startAt": "%s",
                  "endAt": "%s",
                  "isAllDay": false,
                  "color": "OCEAN",
                  "participants": %s,
                  "recurrence": {
                    "freq": "DAILY",
                    "intervalValue": 1,
                    "byDay": null,
                    "byMonthDay": null,
                    "seriesStartAt": "%s",
                    "untilAt": null,
                    "occurrenceCount": 10
                  }
                }
                """.formatted(
                                SERIES_START_AT,
                                SERIES_END_AT,
                                objectMapper.writeValueAsString(participantIds),
                                SERIES_START_AT),
                TeamEventCreateRequest.class);
    }

    private TeamEventUpdateRequest createThisInstanceRequest(List<Long> participantIds) throws JsonProcessingException {
        return createUpdateRequest(
                RecurrenceEditScope.THIS_INSTANCE,
                participantIds,
                targetOccurrenceAt.plusHours(2),
                targetOccurrenceAt.plusHours(3),
                false);
    }

    private TeamEventUpdateRequest createThisAndFollowingRequest(List<Long> participantIds)
            throws JsonProcessingException {
        return createUpdateRequest(
                RecurrenceEditScope.THIS_AND_FOLLOWING,
                participantIds,
                targetOccurrenceAt,
                targetOccurrenceAt.plusHours(1),
                true);
    }

    private TeamEventUpdateRequest createUpdateRequest(
            RecurrenceEditScope editScope,
            List<Long> participantIds,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean includeRecurrence)
            throws JsonProcessingException {
        String recurrenceJson = includeRecurrence ? """
                  {
                    "freq": "DAILY",
                    "intervalValue": 1,
                    "byDay": null,
                    "byMonthDay": null,
                    "seriesStartAt": "%s",
                    "untilAt": null,
                    "occurrenceCount": 8
                  }
                  """.formatted(startAt) : "null";

        return objectMapper.readValue(
                """
                {
                  "title": "수정된 주간 진행 회의",
                  "description": "%s 참여자 변경",
                  "startAt": "%s",
                  "endAt": "%s",
                  "isAllDay": false,
                  "color": "MINT",
                  "isFinished": false,
                  "participants": %s,
                  "recurrenceEditScope": "%s",
                  "occurrenceAt": "%s",
                  "recurrence": %s
                }
                """.formatted(
                                editScope,
                                startAt,
                                endAt,
                                objectMapper.writeValueAsString(participantIds),
                                editScope,
                                targetOccurrenceAt,
                                recurrenceJson),
                TeamEventUpdateRequest.class);
    }

    private void actingAs(User user) {
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }
}
