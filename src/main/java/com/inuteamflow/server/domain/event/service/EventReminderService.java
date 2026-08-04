package com.inuteamflow.server.domain.event.service;

import com.inuteamflow.server.domain.event.dto.Participant;
import com.inuteamflow.server.domain.event.dto.response.EventListResponse;
import com.inuteamflow.server.domain.event.entity.Event;
import com.inuteamflow.server.domain.event.entity.EventParticipant;
import com.inuteamflow.server.domain.event.entity.EventReminderLog;
import com.inuteamflow.server.domain.event.repository.EventParticipantRepository;
import com.inuteamflow.server.domain.event.repository.EventReminderLogRepository;
import com.inuteamflow.server.domain.event.repository.EventRepository;
import com.inuteamflow.server.domain.notification.enums.NotificationType;
import com.inuteamflow.server.domain.notification.service.NotificationService;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventReminderService {

    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final EventOccurrenceService eventOccurrenceService;
    private final EventReminderLogRepository eventReminderLogRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private static final Duration LEAD_TIME = Duration.ofMinutes(10);
    private static final Duration LOOKBACK = Duration.ofMinutes(5);

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 시작이 임박한 팀 일정 회차에 알림을 발송한다.
     *
     * <p>단일 일정과 반복 일정 회차를 수집해 알림 시각이 {@code (기준시각 - LOOKBACK, 기준시각]} 구간에 든 회차만
     * 남기고, 이미 발송한 회차는 이력으로 걸러낸 뒤 참여자에게 알림을 생성하고 발송 이력을 기록한다.</p>
     *
     * <p>반복 회차 계산은 {@link EventOccurrenceService#expandRecurringEvents}를 재사용하므로
     * 취소 회차 제외, 수정 회차의 변경된 시각·참여자 반영, 조회 구간으로 이동해 들어온 회차까지 그대로 처리된다.</p>
     *
     * @param now 스케줄러 실행 시각
     */
    @Transactional
    public void sendDueReminders(LocalDateTime now) {
        LocalDateTime baseAt = now.truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime from = baseAt.minus(LOOKBACK);
        LocalDateTime to = baseAt.plus(LEAD_TIME).plusMinutes(1);

        List<DueOccurrence> candidates = new ArrayList<>();
        collectSingleOccurrences(from, to, candidates);
        int collected = candidates.size();
        collectRecurringOccurrences(from, to, candidates);
        int afterRecurring = candidates.size();

        // 알림 시각이 (from, baseAt] 구간에 든 회차만 발송 대상으로 남긴다.
        candidates.removeIf(candidate -> !isDue(candidate, from, baseAt));
        int due = candidates.size();

        log.info(
                "[reminder] baseAt={} collected(single={}, recurring={}) due={}",
                baseAt,
                collected,
                afterRecurring - collected,
                due);

        if (candidates.isEmpty()) {
            return;
        }

        Set<Long> candidateEventIds =
                candidates.stream().map(DueOccurrence::eventId).collect(Collectors.toSet());
        Set<ReminderKey> alreadySent = eventReminderLogRepository.findByEventIdIn(candidateEventIds).stream()
                .map(log -> new ReminderKey(log.getEventId(), log.getOccurrenceAt()))
                .collect(Collectors.toCollection(HashSet::new));

        for (DueOccurrence candidate : candidates) {
            ReminderKey key = new ReminderKey(candidate.eventId(), candidate.occurrenceAt());
            if (!alreadySent.add(key)) {
                continue;
            }

            List<User> receivers = candidate.receiverUserIds().stream()
                    .distinct()
                    .map(userRepository::getReferenceById)
                    .toList();

            if (!receivers.isEmpty()) {
                notificationService.createSystemNotifications(
                        receivers,
                        buildTitle(),
                        buildContent(candidate),
                        NotificationType.TEAM_SCHEDULE,
                        "/team/" + candidate.teamId(),
                        candidate.createdBy());
            }

            eventReminderLogRepository.save(EventReminderLog.create(candidate.eventId(), candidate.occurrenceAt()));
        }
    }

    /**
     * 발송 대상 단일 팀 일정을 수집한다.
     *
     * <p>단일 일정은 회차가 곧 자기 자신이므로 이력 키({@code occurrenceAt})로 시작 시각을 사용한다.</p>
     *
     * @param from 조회 구간 하한
     * @param to 조회 구간 상한
     * @param candidates 수집한 회차를 추가할 목록
     */
    private void collectSingleOccurrences(LocalDateTime from, LocalDateTime to, List<DueOccurrence> candidates) {
        List<Event> singleEvents = eventRepository.findTeamSingleEventsForReminder(from, to);
        if (singleEvents.isEmpty()) {
            return;
        }

        Map<Long, List<Long>> receiverIdsByEventId = new HashMap<>();
        for (EventParticipant participant : eventParticipantRepository.findByEventInWithMember(singleEvents)) {
            Long eventId = participant.getEvent().getEventId();
            Long userId = participant.getTeamMember().getUser().getUserId();
            receiverIdsByEventId
                    .computeIfAbsent(eventId, key -> new ArrayList<>())
                    .add(userId);
        }

        for (Event event : singleEvents) {
            List<Long> receiverIds = receiverIdsByEventId.getOrDefault(event.getEventId(), List.of());
            candidates.add(new DueOccurrence(
                    event.getEventId(),
                    event.getTeamId(),
                    event.getCreatedBy(),
                    event.getStartAt(),
                    event.getStartAt(),
                    Boolean.TRUE.equals(event.getIsAllDay()),
                    event.getTitle(),
                    receiverIds));
        }
    }

    /**
     * 발송 대상 반복 팀 일정 회차를 수집한다.
     *
     * <p>회차 전개는 {@link EventOccurrenceService#expandRecurringEvents}에 위임하며, 이력 키로는 원래 회차 시각
     * ({@link EventListResponse#getOccurrenceAt()})을, 알림 시각 계산에는 실제 시작 시각
     * ({@link EventListResponse#getStartAt()})을 사용한다.</p>
     *
     * @param from 조회 구간 하한
     * @param to 조회 구간 상한
     * @param candidates 수집한 회차를 추가할 목록
     */
    private void collectRecurringOccurrences(LocalDateTime from, LocalDateTime to, List<DueOccurrence> candidates) {
        List<Event> recurringEvents = eventRepository.findAllTeamRecurringEvents();
        if (recurringEvents.isEmpty()) {
            return;
        }

        Map<Long, Long> createdByByEventId = recurringEvents.stream()
                .collect(Collectors.toMap(Event::getEventId, Event::getCreatedBy, (first, second) -> first));

        List<EventListResponse> occurrences = eventOccurrenceService.expandRecurringEvents(
                recurringEvents, new EventOccurrenceService.DateRange(from, to), null);

        for (EventListResponse occurrence : occurrences) {
            // 회차 단위로 완료 처리된 수정 회차는 제외한다.
            if (Boolean.TRUE.equals(occurrence.getIsFinished())) {
                continue;
            }

            List<Long> receiverIds = occurrence.getParticipants().stream()
                    .map(Participant::getUserId)
                    .toList();

            candidates.add(new DueOccurrence(
                    occurrence.getEventId(),
                    occurrence.getTeamId(),
                    createdByByEventId.get(occurrence.getEventId()),
                    occurrence.getOccurrenceAt(),
                    occurrence.getStartAt(),
                    Boolean.TRUE.equals(occurrence.getIsAllDay()),
                    occurrence.getTitle(),
                    receiverIds));
        }
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 회차의 알림 시각이 발송 구간에 드는지 판단한다.
     *
     * <p>알림 시각은 종일 일정이면 시작 시각(00:00), 시간 일정이면 시작 {@code LEAD_TIME}분 전이다.</p>
     *
     * @param candidate 판단할 회차
     * @param from 발송 구간 하한(미포함)
     * @param baseAt 발송 구간 상한(포함)
     * @return 알림 시각이 {@code (from, baseAt]}에 들면 {@code true}
     */
    private boolean isDue(DueOccurrence candidate, LocalDateTime from, LocalDateTime baseAt) {
        LocalDateTime notifyAt = candidate.allDay()
                ? candidate.startAt()
                : candidate.startAt().minus(LEAD_TIME).truncatedTo(ChronoUnit.MINUTES);
        return notifyAt.isAfter(from) && !notifyAt.isAfter(baseAt);
    }

    /**
     * 시스템 알림의 제목을 생성한다.
     *
     * @return {@code "곧 시작하는 일정이 있어요"}
     */
    private String buildTitle() {
        return "곧 시작하는 일정이 있어요";
    }

    /**
     * 시스템 알림의 내용을 생성한다.
     *
     * @param candidate 알림을 전송할 회차
     * @return 회차가 하루 종일이라면 {@code "오늘 ~일정이 있어요"}, 아니라면 {@code "~일정이 ~분 후 시작해요"}
     */
    private String buildContent(DueOccurrence candidate) {
        if (candidate.allDay()) {
            return "오늘 '" + candidate.title() + "' 일정이 있어요";
        }
        return "'" + candidate.title() + "' 일정이 " + LEAD_TIME.toMinutes() + "분 후 시작해요";
    }

    private record DueOccurrence(
            Long eventId,
            Long teamId,
            Long createdBy,
            LocalDateTime occurrenceAt,
            LocalDateTime startAt,
            boolean allDay,
            String title,
            List<Long> receiverUserIds) {}

    private record ReminderKey(Long eventId, LocalDateTime occurrenceAt) {}
}
