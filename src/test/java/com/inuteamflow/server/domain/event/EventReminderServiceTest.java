package com.inuteamflow.server.domain.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inuteamflow.server.domain.event.dto.request.MyEventCreateRequest;
import com.inuteamflow.server.domain.event.dto.request.TeamEventCreateRequest;
import com.inuteamflow.server.domain.event.dto.request.TeamEventUpdateRequest;
import com.inuteamflow.server.domain.event.dto.response.EventDetailResponse;
import com.inuteamflow.server.domain.event.entity.EventReminderLog;
import com.inuteamflow.server.domain.event.enums.RecurrenceEditScope;
import com.inuteamflow.server.domain.event.repository.EventReminderLogRepository;
import com.inuteamflow.server.domain.event.service.EventReminderService;
import com.inuteamflow.server.domain.event.service.MyEventService;
import com.inuteamflow.server.domain.event.service.TeamEventService;
import com.inuteamflow.server.domain.notification.enums.NotificationType;
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
 * {@link EventReminderService#sendDueReminders}가 조회 창 밖에서 시작한 반복 일정의 회차를
 * 창 안으로 들여 발송 대상으로 잡는지 검증한다.
 *
 * <p>알림 판정 기준: 시간 일정은 시작 {@code LEAD_TIME}(10분) 전이 발송 시각이고, 발송 구간은
 * {@code (기준시각 - LOOKBACK(5분), 기준시각]}이다. 따라서 {@code now=15:00}이면 시작 시각이
 * {@code 15:10}인 회차가 발송 대상이 된다.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EventReminderServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 4, 15, 0);
    private static final LocalDateTime IN_WINDOW_START = LocalDateTime.of(2026, 8, 4, 15, 10);

    @Autowired
    private EventReminderService eventReminderService;

    @Autowired
    private TeamEventService teamEventService;

    @Autowired
    private MyEventService myEventService;

    @Autowired
    private EventReminderLogRepository eventReminderLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationService notificationService;

    private User hostUser;

    private Team team;
    private TeamMember host;
    private TeamMember participantA;
    private TeamMember participantB;

    @BeforeEach
    void setUp() {
        hostUser = saveUser("reminder-host");
        User participantAUser = saveUser("reminder-participant-a");
        User participantBUser = saveUser("reminder-participant-b");

        actingAs(hostUser);
        team = teamRepository.saveAndFlush(Team.builder()
                .name("리마인더 테스트 팀")
                .description("반복 일정 회차 리마인더 검증")
                .category(Category.PROJECT)
                .build());

        host = teamMemberRepository.save(TeamMember.create(team, hostUser, TeamRole.LEADER));
        participantA = teamMemberRepository.save(TeamMember.create(team, participantAUser, TeamRole.MEMBER));
        participantB = teamMemberRepository.save(TeamMember.create(team, participantBUser, TeamRole.MEMBER));
        teamMemberRepository.flush();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("THIS_INSTANCE로 미래 회차를 알림 창 안으로 옮기면, 그 회차에 리마인더가 발송되고 원래 슬롯 키로 기록된다.")
    void reminder_sendsForOccurrenceMovedIntoWindowByThisInstance() throws JsonProcessingException {
        LocalDateTime seriesStart = LocalDateTime.of(2026, 8, 4, 10, 0);
        LocalDateTime futureOccurrence = LocalDateTime.of(2026, 8, 11, 10, 0);

        Long eventId = teamEventService
                .createTeamEvent(
                        hostUser,
                        team.getTeamId(),
                        createRecurringEventRequest(seriesStart, seriesStart.plusHours(1), 30, participantIds()))
                .getEventId();

        teamEventService.updateTeamEvent(
                hostUser,
                team.getTeamId(),
                eventId,
                createUpdateRequest(
                        RecurrenceEditScope.THIS_INSTANCE,
                        futureOccurrence,
                        IN_WINDOW_START,
                        IN_WINDOW_START.plusHours(1),
                        participantIds(),
                        null));

        entityManager.flush();
        entityManager.clear();

        eventReminderService.sendDueReminders(NOW);

        entityManager.flush();
        entityManager.clear();

        verify(notificationService, times(1))
                .createSystemNotifications(
                        anyList(),
                        eq("곧 시작하는 일정이 있어요"),
                        anyString(),
                        eq(NotificationType.CALENDAR),
                        eq("/team/" + team.getTeamId()),
                        any());

        List<EventReminderLog> logs = eventReminderLogRepository.findAll();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getEventId()).isEqualTo(eventId);
        assertThat(logs.get(0).getOccurrenceAt()).isEqualTo(futureOccurrence);
    }

    @Test
    @DisplayName("THIS_AND_FOLLOWING으로 과거에 시작한 시리즈의 회차가 알림 창에 들면, 그 회차에 리마인더가 발송된다.")
    void reminder_sendsForFollowingSeriesOccurrenceInWindow() throws JsonProcessingException {
        LocalDateTime seriesStart = LocalDateTime.of(2026, 8, 1, 15, 10);
        // 과거 회차부터 분리 → following 시리즈 시작 시각(8/2)은 과거지만, 그 회차(8/4 15:10)가 알림 창에 든다.
        LocalDateTime splitTarget = LocalDateTime.of(2026, 8, 2, 15, 10);

        Long originalEventId = teamEventService
                .createTeamEvent(
                        hostUser,
                        team.getTeamId(),
                        createRecurringEventRequest(seriesStart, seriesStart.plusHours(1), 10, participantIds()))
                .getEventId();

        EventDetailResponse following = teamEventService.updateTeamEvent(
                hostUser,
                team.getTeamId(),
                originalEventId,
                createUpdateRequest(
                        RecurrenceEditScope.THIS_AND_FOLLOWING,
                        splitTarget,
                        splitTarget,
                        splitTarget.plusHours(1),
                        participantIds(),
                        8));

        entityManager.flush();
        entityManager.clear();

        eventReminderService.sendDueReminders(NOW);

        entityManager.flush();
        entityManager.clear();

        verify(notificationService, times(1))
                .createSystemNotifications(
                        anyList(),
                        eq("곧 시작하는 일정이 있어요"),
                        anyString(),
                        eq(NotificationType.CALENDAR),
                        eq("/team/" + team.getTeamId()),
                        any());

        List<EventReminderLog> logs = eventReminderLogRepository.findAll();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getEventId()).isEqualTo(following.getEventId());
        assertThat(logs.get(0).getOccurrenceAt()).isEqualTo(IN_WINDOW_START);
    }

    @Test
    @DisplayName("시작이 임박한 단일 개인 일정은 생성자 본인에게 CALENDAR 알림으로 발송되고 시작 슬롯 키로 기록된다.")
    void reminder_sendsForPersonalSingleEventInWindow() throws JsonProcessingException {
        Long eventId = myEventService
                .createMyEvent(hostUser, createPersonalEventRequest(IN_WINDOW_START, IN_WINDOW_START.plusHours(1)))
                .getEventId();

        entityManager.flush();
        entityManager.clear();

        eventReminderService.sendDueReminders(NOW);

        entityManager.flush();
        entityManager.clear();

        verify(notificationService, times(1))
                .createSystemNotifications(
                        anyList(),
                        eq("곧 시작하는 일정이 있어요"),
                        anyString(),
                        eq(NotificationType.CALENDAR),
                        eq("/calendar"),
                        any());

        List<EventReminderLog> logs = eventReminderLogRepository.findAll();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getEventId()).isEqualTo(eventId);
        assertThat(logs.get(0).getOccurrenceAt()).isEqualTo(IN_WINDOW_START);
    }

    private MyEventCreateRequest createPersonalEventRequest(LocalDateTime startAt, LocalDateTime endAt)
            throws JsonProcessingException {
        return objectMapper.readValue("""
                {
                  "title": "개인 스터디",
                  "description": "개인 일정 리마인더 테스트",
                  "startAt": "%s",
                  "endAt": "%s",
                  "isAllDay": false,
                  "color": "LAVENDER",
                  "recurrence": null
                }
                """.formatted(startAt, endAt), MyEventCreateRequest.class);
    }

    private List<Long> participantIds() {
        return List.of(participantA.getTeamMemberId(), participantB.getTeamMemberId());
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

    private TeamEventCreateRequest createRecurringEventRequest(
            LocalDateTime startAt, LocalDateTime endAt, int occurrenceCount, List<Long> participantIds)
            throws JsonProcessingException {
        return objectMapper.readValue(
                """
                {
                  "title": "반복 회의",
                  "description": "리마인더 테스트",
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
                    "occurrenceCount": %d
                  }
                }
                """.formatted(
                        startAt, endAt, objectMapper.writeValueAsString(participantIds), startAt, occurrenceCount),
                TeamEventCreateRequest.class);
    }

    private TeamEventUpdateRequest createUpdateRequest(
            RecurrenceEditScope editScope,
            LocalDateTime occurrenceAt,
            LocalDateTime startAt,
            LocalDateTime endAt,
            List<Long> participantIds,
            Integer followingCount)
            throws JsonProcessingException {
        String recurrenceJson = followingCount == null ? "null" : """
                  {
                    "freq": "DAILY",
                    "intervalValue": 1,
                    "byDay": null,
                    "byMonthDay": null,
                    "seriesStartAt": "%s",
                    "untilAt": null,
                    "occurrenceCount": %d
                  }
                  """.formatted(startAt, followingCount);

        return objectMapper.readValue(
                """
                {
                  "title": "수정된 반복 회의",
                  "description": "리마인더 테스트 회차 수정",
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
                                startAt,
                                endAt,
                                objectMapper.writeValueAsString(participantIds),
                                editScope,
                                occurrenceAt,
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
