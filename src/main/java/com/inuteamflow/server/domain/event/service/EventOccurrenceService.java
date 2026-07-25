package com.inuteamflow.server.domain.event.service;

import com.inuteamflow.server.domain.event.dto.Participant;
import com.inuteamflow.server.domain.event.dto.response.EventListResponse;
import com.inuteamflow.server.domain.event.entity.Event;
import com.inuteamflow.server.domain.event.entity.RecurrenceException;
import com.inuteamflow.server.domain.event.entity.RecurrenceRule;
import com.inuteamflow.server.domain.event.enums.RecurrenceExceptionType;
import com.inuteamflow.server.domain.event.enums.RecurrenceFrequency;
import com.inuteamflow.server.domain.event.repository.EventParticipantRepository;
import com.inuteamflow.server.domain.event.repository.RecurrenceExceptionParticipantRepository;
import com.inuteamflow.server.domain.event.repository.RecurrenceExceptionRepository;
import com.inuteamflow.server.domain.event.repository.RecurrenceRuleRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventOccurrenceService {

    private final RecurrenceRuleRepository recurrenceRuleRepository;
    private final RecurrenceExceptionRepository recurrenceExceptionRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final RecurrenceExceptionParticipantRepository recurrenceExceptionParticipantRepository;

    // year/month 요청값을 실제 조회 범위인 [이번 달 1일 00:00, 다음 달 1일 00:00]로 변환한다.
    public DateRange createMonthlyDateRange(
            Integer year,
            Integer month
    ) {
        if (year == null || month == null || month < 1 || month > 12) {
            throw new RestApiException(CustomErrorCode.EVENT_MONTH_INVALID);
        }

        YearMonth yearMonth = YearMonth.of(year, month);
        return new DateRange(
                yearMonth.atDay(1).atStartOfDay(),
                yearMonth.plusMonths(1).atDay(1).atStartOfDay()
        );
    }

    // 반복 일정 후보들의 rule/exception을 한 번에 조회한 뒤, 각 반복 일정을 실제 occurrence 목록으로 펼친다.
    // user가 non-null이면 RecurrenceExceptionParticipant 기준으로 참석자 필터링을 적용한다.
    public List<EventListResponse> expandRecurringEvents(
            List<Event> recurringEvents,
            EventOccurrenceService.DateRange dateRange,
            User requester,
            boolean filterByParticipation
    ) {
        if (recurringEvents.isEmpty()) {
            return List.of();
        }

        Map<Long, RecurrenceRule> ruleByEventId =
                recurrenceRuleRepository
                        .findByEventIn(recurringEvents).stream()
                        .collect(Collectors.toMap(RecurrenceRule::getEventId, Function.identity()));

        List<RecurrenceException> recurrenceExceptions =
                recurrenceExceptionRepository.findByEventIn(recurringEvents);
        List<RecurrenceException> movedIntoRangeExceptions =
                recurrenceExceptionRepository.findModifiedOverlapping(
                        RecurrenceExceptionType.MODIFIED,
                        recurringEvents,
                        dateRange.startAt(),
                        dateRange.endAt()
                );

        Map<EventOccurrenceService.OccurrenceKey, RecurrenceException> exceptionByKey =
                recurrenceExceptions.stream()
                        .collect(Collectors.toMap(
                                exception -> new EventOccurrenceService.OccurrenceKey(
                                        exception.getEventId(),
                                        exception.getOriginalOccurrenceAt()
                                ),
                                Function.identity(),
                                (first, second) -> second
                        ));

        Map<Long, List<Participant>> participantsByEventId =
                createParticipantsByEventId(recurringEvents);
        Map<Long, List<Participant>> participantsByExceptionId =
                createParticipantsByExceptionId(recurrenceExceptions);

        List<EventListResponse> responses = new ArrayList<>();
        for (Event event : recurringEvents) {
            responses.addAll(expandRecurringEvent(
                    event,
                    ruleByEventId.get(event.getEventId()),
                    exceptionByKey,
                    dateRange,
                    participantsByEventId,
                    participantsByExceptionId,
                    requester,
                    filterByParticipation
            ));
        }

        Set<OccurrenceKey> responseKeys = responses.stream()
                .filter(response -> response.getOccurrenceAt() != null)
                .map(response -> new OccurrenceKey(response.getEventId(), response.getOccurrenceAt()))
                .collect(Collectors.toSet());

        for (RecurrenceException recurrenceException : movedIntoRangeExceptions) {
            OccurrenceKey occurrenceKey = new OccurrenceKey(recurrenceException.getEventId(), recurrenceException.getOriginalOccurrenceAt());

            if (responseKeys.contains(occurrenceKey)) continue;

            addExceptionOccurrence(
                    recurrenceException.getEvent(),
                    ruleByEventId.get(recurrenceException.getEventId()),
                    recurrenceException,
                    dateRange,
                    responses,
                    participantsByExceptionId,
                    requester,
                    filterByParticipation
            );
        }

        return responses;
    }

    public List<EventListResponse> expandRecurringEvents(
            List<Event> recurringEvents,
            EventOccurrenceService.DateRange dateRange,
            User requester
    ) {
        return expandRecurringEvents(recurringEvents, dateRange, requester, false);
    }

    // 반복 일정 하나를 조회 범위 안의 occurrence 들로 변환한다. DB row를 만들지 않고 메모리에서만 계산한다.
    public List<EventListResponse> expandRecurringEvent(
            Event event,
            RecurrenceRule rule,
            Map<OccurrenceKey, RecurrenceException> exceptionByKey,
            DateRange dateRange,
            Map<Long, List<Participant>> participantsByEventId,
            Map<Long, List<Participant>> participantsByExceptionId,
            User requester,
            boolean filterByParticipation
    ) {
        if (rule == null || !canAffectDateRange(rule, dateRange)) {
            return List.of();
        }

        List<EventListResponse> responses = new ArrayList<>();

        // 반복 계산 시작 시각:
        // seriesStartAt이 있으면 사용하고, 없으면 event.startAt 기준으로 시작
        LocalDateTime occurrenceAt = firstOccurrenceAt(event, rule);

        // 원본 이벤트의 duration(start~end 차이)을 유지하기 위해 초 단위로 계산
        long durationSeconds = Duration.between(event.getStartAt(), event.getEndAt()).getSeconds();
        int generatedCount = 0;

        // 조회 종료 시각 이전까지 반복 occurrence 생성
        while (occurrenceAt.isBefore(dateRange.endAt())) {
            generatedCount++;

            // untilAt 또는 occurrenceCount 조건을 초과하면 종료
            if (isAfterRecurrenceEnd(rule, occurrenceAt, generatedCount)) {
                break;
            }

            // 현재 회차에 대응되는 recurrence_exception 조회
            // Key: (eventId + originalOccurrenceAt)
            RecurrenceException recurrenceException = exceptionByKey.get(
                    new OccurrenceKey(event.getEventId(), occurrenceAt)
            );
            if (recurrenceException != null) {
                // 예외 회차가 존재하면: - CANCELLED → 제외, - MODIFIED → 수정된 값 기준으로 응답 생성
                addExceptionOccurrence(
                        event,
                        rule,
                        recurrenceException,
                        dateRange,
                        responses,
                        participantsByExceptionId,
                        requester,
                        filterByParticipation
                );
            } else {
                // 예외가 없으면 일반 반복 occurrence 생성
                addNormalOccurrence(
                        event,
                        rule,
                        occurrenceAt,
                        durationSeconds,
                        dateRange,
                        responses,
                        participantsByEventId,
                        requester
                );
            }

            occurrenceAt = nextOccurrenceAt(occurrenceAt, rule);
        }

        return responses;
    }

    // 단건 일정 응답과 반복 occurrence 응답을 합친 뒤, 캘린더 표시용으로 시작 시간 기준 정렬한다.
    public List<EventListResponse> mergeAndSort(
            List<Event> singleEvents,
            List<EventListResponse> recurringOccurrences,
            User requester
    ) {
        List<EventListResponse> responses = new ArrayList<>();
        Map<Long, List<Participant>> participantsByEventId =
                createParticipantsByEventId(singleEvents);
        singleEvents.stream()
                .map(event -> {
                    List<Participant> participants = participantsByEventId.getOrDefault(
                            event.getEventId(),
                            List.of()
                    );
                    return EventListResponse.createSingle(
                            event,
                            Participant.isParticipant(participants, requester),
                            participants
                    );
                })
                .forEach(responses::add);
        responses.addAll(recurringOccurrences);
        responses.sort(Comparator.comparing(EventListResponse::getStartAt));

        return responses;
    }

    // 예외가 없는 일반 반복 회차를 응답에 추가한다. 조회 범위와 겹치지 않으면 제외한다.
    public boolean existsOccurrence(
            Event event,
            RecurrenceRule rule,
            LocalDateTime occurrenceAt
    ) {
        if (rule == null || occurrenceAt == null) {
            return false;
        }

        LocalDateTime currentOccurrenceAt = firstOccurrenceAt(event, rule);
        int generatedCount = 0;

        while (!currentOccurrenceAt.isAfter(occurrenceAt)) {
            generatedCount++;
            if (isAfterRecurrenceEnd(rule, currentOccurrenceAt, generatedCount)) {
                return false;
            }

            if (currentOccurrenceAt.equals(occurrenceAt)) {
                return true;
            }

            currentOccurrenceAt = nextOccurrenceAt(currentOccurrenceAt, rule);
        }

        return false;
    }

    private void addNormalOccurrence(
            Event event,
            RecurrenceRule rule,
            LocalDateTime occurrenceAt,
            long durationSeconds,
            DateRange dateRange,
            List<EventListResponse> responses,
            Map<Long, List<Participant>> participantsByEventId,
            User requester
    ) {
        LocalDateTime startAt = occurrenceAt;
        LocalDateTime endAt = occurrenceAt.plusSeconds(durationSeconds);
        if (isOverlapped(startAt, endAt, dateRange)) {
            List<Participant> participants = participantsByEventId.getOrDefault(
                    event.getEventId(),
                    List.of()
            );
            responses.add(EventListResponse.createOccurrence(
                    event,
                    rule,
                    occurrenceAt,
                    startAt,
                    endAt,
                    Participant.isParticipant(participants, requester),
                    participants
            ));
        }
    }

    // 반복 예외를 반영한다. CANCELLED는 제외하고, MODIFIED는 수정된 시간 기준으로 조회 범위와 겹칠 때 추가한다.
    // participatingExceptionIds가 non-null이면 해당 유저가 참석자인 예외 회차만 포함한다.
    private void addExceptionOccurrence(
            Event event,
            RecurrenceRule rule,
            RecurrenceException recurrenceException,
            DateRange dateRange,
            List<EventListResponse> responses,
            Map<Long, List<Participant>> participantsByExceptionId,
            User requester,
            boolean filterByParticipation
    ) {
        if (recurrenceException.getExceptionType() == RecurrenceExceptionType.CANCELLED) {
            return;
        }

        List<Participant> participants = participantsByExceptionId.getOrDefault(
                recurrenceException.getRecurrenceExceptionId(),
                List.of()
        );
        boolean isParticipant = Participant.isParticipant(participants, requester);
        if (filterByParticipation && !isParticipant) {
            return;
        }

        if (recurrenceException.getExceptionType() == RecurrenceExceptionType.MODIFIED
                && isOverlapped(
                recurrenceException.getModifiedStartAt(),
                recurrenceException.getModifiedEndAt(),
                dateRange
        )) {
            responses.add(EventListResponse.createModifiedOccurrence(
                    event,
                    rule,
                    recurrenceException,
                    isParticipant,
                    participants
            ));
        }
    }

    private Map<Long, List<Participant>> createParticipantsByEventId(
            List<Event> events
    ) {
        if (events.isEmpty()) {
            return Map.of();
        }

        return eventParticipantRepository.findByEventInWithMember(events).stream()
                .collect(Collectors.groupingBy(
                        participant -> participant.getEvent().getEventId(),
                        Collectors.mapping(Participant::create, Collectors.toList())
                ));
    }

    private Map<Long, List<Participant>> createParticipantsByExceptionId(
            List<RecurrenceException> recurrenceExceptions
    ) {
        if (recurrenceExceptions.isEmpty()) {
            return Map.of();
        }

        return recurrenceExceptionParticipantRepository
                .findByRecurrenceExceptionInWithMember(recurrenceExceptions)
                .stream()
                .collect(Collectors.groupingBy(
                        participant -> participant.getRecurrenceException().getRecurrenceExceptionId(),
                        Collectors.mapping(Participant::create, Collectors.toList())
                ));
    }

    // 반복 규칙이 조회 시작일 이후에도 영향을 줄 가능성이 있는지 확인한다.
    private boolean canAffectDateRange(
            RecurrenceRule rule,
            DateRange dateRange
    ) {
        return rule.getUntilAt() == null || !rule.getUntilAt().isBefore(dateRange.startAt());
    }

    // untilAt 또는 occurrenceCount 기준으로 더 이상 회차를 만들면 안 되는지 확인한다.
    private boolean isAfterRecurrenceEnd(
            RecurrenceRule rule,
            LocalDateTime occurrenceAt,
            int generatedCount
    ) {
        if (rule.getOccurrenceCount() != null && generatedCount > rule.getOccurrenceCount()) {
            return true;
        }

        return rule.getUntilAt() != null && occurrenceAt.isAfter(rule.getUntilAt());
    }

    // 현재 occurrence 기준으로 다음 회차 시작 시간을 계산한다.
    private LocalDateTime nextOccurrenceAt(
            LocalDateTime occurrenceAt,
            RecurrenceRule rule
    ) {
        int interval = rule.getIntervalValue();
        RecurrenceFrequency frequency = rule.getFreq();

        return switch (frequency) {
            case DAILY -> occurrenceAt.plusDays(interval);
            case WEEKLY -> nextWeeklyOccurrenceAt(occurrenceAt, interval, rule);
            case MONTHLY -> nextMonthlyOccurrenceAt(occurrenceAt, interval, rule);
            case YEARLY -> occurrenceAt.plusYears(interval);
        };
    }

    // 월 반복에서 31일처럼 다음 달에 없는 날짜는 해당 달의 마지막 날짜로 보정한다.
    // 주간 반복은 seriesStartAt 이후 첫 byDay 부터 시작한다.
    private LocalDateTime firstOccurrenceAt(
            Event event,
            RecurrenceRule rule
    ) {
        LocalDateTime seriesStartAt = rule.getSeriesStartAt() != null
                ? rule.getSeriesStartAt()
                : event.getStartAt();

        if (rule.getFreq() != RecurrenceFrequency.WEEKLY || sortedByDays(rule).isEmpty()) {
            return seriesStartAt;
        }

        return firstWeeklyOccurrenceAt(seriesStartAt, rule);
    }

    private LocalDateTime nextWeeklyOccurrenceAt(
            LocalDateTime occurrenceAt,
            int interval,
            RecurrenceRule rule
    ) {
        List<DayOfWeek> byDays = sortedByDays(rule);
        if (byDays.isEmpty()) {
            return occurrenceAt.plusWeeks(interval);
        }

        DayOfWeek currentDay = occurrenceAt.getDayOfWeek();
        return byDays.stream()
                .filter(dayOfWeek -> dayOfWeek.getValue() > currentDay.getValue())
                .findFirst()
                .map(nextDay -> moveToDayOfWeek(occurrenceAt, nextDay))
                .orElseGet(() -> moveToDayOfWeek(
                        occurrenceAt.plusWeeks(interval).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                        byDays.get(0)
                ));
    }

    private LocalDateTime firstWeeklyOccurrenceAt(
            LocalDateTime seriesStartAt,
            RecurrenceRule rule
    ) {
        return sortedByDays(rule).stream()
                .map(dayOfWeek -> moveToDayOfWeek(seriesStartAt, dayOfWeek))
                .min(Comparator.naturalOrder())
                .orElse(seriesStartAt);
    }

    private List<DayOfWeek> sortedByDays(
            RecurrenceRule rule
    ) {
        return rule.getByDay().stream()
                .distinct()
                .sorted(Comparator.comparingInt(DayOfWeek::getValue))
                .toList();
    }

    private LocalDateTime moveToDayOfWeek(
            LocalDateTime dateTime,
            DayOfWeek dayOfWeek
    ) {
        return dateTime.with(TemporalAdjusters.nextOrSame(dayOfWeek));
    }

    // 월 반복에서 다음 달에 없는 날짜는 해당 달의 마지막 날짜로 보정한다.
    private LocalDateTime nextMonthlyOccurrenceAt(
            LocalDateTime occurrenceAt,
            int interval,
            RecurrenceRule rule
    ) {
        LocalDateTime next = occurrenceAt.plusMonths(interval);
        if (rule.getByMonthDay() == null) {
            return next;
        }

        int dayOfMonth = Math.min(rule.getByMonthDay(), next.toLocalDate().lengthOfMonth());
        return next.withDayOfMonth(dayOfMonth);
    }

    // [startAt, endAt) 일정 구간이 조회 범위와 겹치는지 판단한다.
    private boolean isOverlapped(
            LocalDateTime startAt,
            LocalDateTime endAt,
            DateRange dateRange
    ) {
        if (startAt == null || endAt == null) {
            return false;
        }

        return startAt.isBefore(dateRange.endAt()) && endAt.isAfter(dateRange.startAt());
    }

    public record DateRange(
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
    }

    public record OccurrenceKey(
            Long eventId,
            LocalDateTime originalOccurrenceAt
    ) {
    }
}
