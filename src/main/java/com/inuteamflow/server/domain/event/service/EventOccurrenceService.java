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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventOccurrenceService {

    private final RecurrenceRuleRepository recurrenceRuleRepository;
    private final RecurrenceExceptionRepository recurrenceExceptionRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final RecurrenceExceptionParticipantRepository recurrenceExceptionParticipantRepository;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 연도와 월을 월간 조회 범위로 변환한다.
     *
     * <p>조회 범위는 해당 월의 1일 00:00을 포함하고 다음 달의 1일 00:00은 포함하지 않는다.</p>
     *
     * @param year 조회할 연도
     * @param month 조회할 월
     * @return 월의 시작 시각부터 다음 달 시작 시각까지의 조회 범위
     * @throws RestApiException 연도나 월이 없거나 월이 유효하지 않은 경우
     */
    public DateRange createMonthlyDateRange(Integer year, Integer month) {
        if (year == null || month == null || month < 1 || month > 12) {
            throw new RestApiException(CustomErrorCode.EVENT_MONTH_INVALID);
        }

        YearMonth yearMonth = YearMonth.of(year, month);
        return new DateRange(
                yearMonth.atDay(1).atStartOfDay(),
                yearMonth.plusMonths(1).atDay(1).atStartOfDay());
    }

    /**
     * 여러 반복 일정을 조회 범위의 회차 목록으로 확장한다.
     *
     * <p>참여 여부를 별도로 제한하지 않고 모든 정상 회차와 수정 예외 회차를 반환한다.</p>
     *
     * <p>수정되어 조회 범위 안으로 이동한 예외 회차도 포함하며 요청자의 참여 여부로 필터링할 수 있다.</p>
     *
     * @param recurringEvents 확장할 반복 일정 목록
     * @param dateRange 회차를 조회할 날짜 범위
     * @param requester 일정을 조회하는 사용자
     * @param filterByParticipation 참여 회차만 포함할지 여부
     * @return 조회 범위에 포함되는 반복 회차 목록
     */
    public List<EventListResponse> expandRecurringEvents(
            List<Event> recurringEvents,
            EventOccurrenceService.DateRange dateRange,
            User requester,
            boolean filterByParticipation) {
        if (recurringEvents.isEmpty()) {
            return List.of();
        }

        Map<Long, RecurrenceRule> ruleByEventId = recurrenceRuleRepository.findByEventIn(recurringEvents).stream()
                .collect(Collectors.toMap(RecurrenceRule::getEventId, Function.identity()));

        List<RecurrenceException> recurrenceExceptions = recurrenceExceptionRepository.findByEventIn(recurringEvents);
        List<RecurrenceException> movedIntoRangeExceptions = recurrenceExceptionRepository.findModifiedOverlapping(
                RecurrenceExceptionType.MODIFIED, recurringEvents, dateRange.startAt(), dateRange.endAt());

        Map<EventOccurrenceService.OccurrenceKey, RecurrenceException> exceptionByKey = recurrenceExceptions.stream()
                .collect(Collectors.toMap(
                        exception -> new EventOccurrenceService.OccurrenceKey(
                                exception.getEventId(), exception.getOriginalOccurrenceAt()),
                        Function.identity(),
                        (first, second) -> second));

        Map<Long, List<Participant>> participantsByEventId = createParticipantsByEventId(recurringEvents);
        Map<Long, List<Participant>> participantsByExceptionId = createParticipantsByExceptionId(recurrenceExceptions);

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
                    filterByParticipation));
        }

        Set<OccurrenceKey> responseKeys = responses.stream()
                .filter(response -> response.getOccurrenceAt() != null)
                .map(response -> new OccurrenceKey(response.getEventId(), response.getOccurrenceAt()))
                .collect(Collectors.toSet());

        for (RecurrenceException recurrenceException : movedIntoRangeExceptions) {
            OccurrenceKey occurrenceKey =
                    new OccurrenceKey(recurrenceException.getEventId(), recurrenceException.getOriginalOccurrenceAt());

            if (responseKeys.contains(occurrenceKey)) continue;

            addExceptionOccurrence(
                    recurrenceException.getEvent(),
                    ruleByEventId.get(recurrenceException.getEventId()),
                    recurrenceException,
                    dateRange,
                    responses,
                    participantsByExceptionId,
                    requester,
                    filterByParticipation);
        }

        return responses;
    }

    /**
     * 여러 반복 일정을 조회 범위의 회차 목록으로 확장한다.
     *
     * @param recurringEvents 확장할 반복 일정 목록
     * @param dateRange 회차를 조회할 날짜 범위
     * @param requester 일정을 조회하는 사용자
     * @return 조회 범위에 포함되는 반복 회차 목록
     */
    public List<EventListResponse> expandRecurringEvents(
            List<Event> recurringEvents, EventOccurrenceService.DateRange dateRange, User requester) {
        return expandRecurringEvents(recurringEvents, dateRange, requester, false);
    }

    /**
     * 반복 일정 하나를 조회 범위의 회차 목록으로 확장한다.
     *
     * <p>데이터베이스에 회차를 생성하지 않고 반복 규칙의 종료 시각과 횟수, 취소·수정 예외를 반영하여 계산한다.</p>
     *
     * @param event 확장할 반복 일정
     * @param rule 일정의 반복 규칙
     * @param exceptionByKey 일정과 원래 회차 시각별 반복 예외
     * @param dateRange 회차를 조회할 날짜 범위
     * @param participantsByEventId 일정 ID별 참여자 목록
     * @param participantsByExceptionId 반복 예외 ID별 참여자 목록
     * @param requester 일정을 조회하는 사용자
     * @param filterByParticipation 참여 회차만 포함할지 여부
     * @return 조회 범위에 포함되는 반복 회차 목록
     */
    public List<EventListResponse> expandRecurringEvent(
            Event event,
            RecurrenceRule rule,
            Map<OccurrenceKey, RecurrenceException> exceptionByKey,
            DateRange dateRange,
            Map<Long, List<Participant>> participantsByEventId,
            Map<Long, List<Participant>> participantsByExceptionId,
            User requester,
            boolean filterByParticipation) {
        if (rule == null || !canAffectDateRange(rule, dateRange)) {
            return List.of();
        }

        List<EventListResponse> responses = new ArrayList<>();

        LocalDateTime occurrenceAt = firstOccurrenceAt(event, rule);

        // 원본 이벤트의 duration(start~end 차이)을 유지하기 위해 초 단위로 계산
        long durationSeconds =
                Duration.between(event.getStartAt(), event.getEndAt()).toSeconds();
        int generatedCount = 0;

        // 조회 종료 시각 이전까지 반복 occurrence 생성
        while (occurrenceAt.isBefore(dateRange.endAt())) {
            generatedCount++;

            if (isAfterRecurrenceEnd(rule, occurrenceAt, generatedCount)) {
                break;
            }

            RecurrenceException recurrenceException =
                    exceptionByKey.get(new OccurrenceKey(event.getEventId(), occurrenceAt));
            if (recurrenceException != null) {
                addExceptionOccurrence(
                        event,
                        rule,
                        recurrenceException,
                        dateRange,
                        responses,
                        participantsByExceptionId,
                        requester,
                        filterByParticipation);
            } else {
                addNormalOccurrence(
                        event,
                        rule,
                        occurrenceAt,
                        durationSeconds,
                        dateRange,
                        responses,
                        participantsByEventId,
                        requester);
            }

            occurrenceAt = nextOccurrenceAt(occurrenceAt, rule);
        }

        return responses;
    }

    /**
     * 단일 일정과 반복 회차를 병합하여 정렬한다.
     *
     * <p>단일 일정에는 일정 참여자 정보를 결합하고 전체 결과를 시작 시각 오름차순으로 정렬한다.</p>
     *
     * @param singleEvents 병합할 단일 일정 목록
     * @param recurringOccurrences 병합할 반복 회차 목록
     * @param requester 일정을 조회하는 사용자
     * @return 시작 시간 오름차순으로 정렬된 일정 목록
     */
    public List<EventListResponse> mergeAndSort(
            List<Event> singleEvents, List<EventListResponse> recurringOccurrences, User requester) {
        List<EventListResponse> responses = new ArrayList<>();
        Map<Long, List<Participant>> participantsByEventId = createParticipantsByEventId(singleEvents);
        singleEvents.stream()
                .map(event -> {
                    List<Participant> participants = participantsByEventId.getOrDefault(event.getEventId(), List.of());
                    return EventListResponse.createSingle(
                            event, Participant.isParticipant(participants, requester), participants);
                })
                .forEach(responses::add);
        responses.addAll(recurringOccurrences);
        responses.sort(Comparator.comparing(EventListResponse::getStartAt));

        return responses;
    }

    /**
     * 지정 시각에 반복 회차가 존재하는지 확인한다.
     *
     * <p>첫 회차부터 순차적으로 계산하며 반복 종료 시각 또는 최대 발생 횟수를 넘으면 탐색을 종료한다.</p>
     *
     * @param event 확인할 반복 일정
     * @param rule 일정의 반복 규칙
     * @param occurrenceAt 확인할 회차 시작 시각
     * @return 반복 회차가 존재하면 {@code true}, 그렇지 않으면 {@code false}
     */
    public boolean existsOccurrence(Event event, RecurrenceRule rule, LocalDateTime occurrenceAt) {
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

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 일반 반복 회차가 조회 범위와 겹치면 응답에 추가한다.
     *
     * <p>원본 일정의 지속 시간을 회차 시작 시각에 적용하고 일정 참여자 정보를 응답에 포함한다.</p>
     *
     * @param event 회차의 원본 일정
     * @param rule 일정의 반복 규칙
     * @param occurrenceAt 회차의 시작 시각
     * @param durationSeconds 일정의 지속 시간
     * @param dateRange 회차를 조회할 날짜 범위
     * @param responses 회차를 추가할 응답 목록
     * @param participantsByEventId 일정 ID별 참여자 목록
     * @param requester 일정을 조회하는 사용자
     */
    private void addNormalOccurrence(
            Event event,
            RecurrenceRule rule,
            LocalDateTime occurrenceAt,
            long durationSeconds,
            DateRange dateRange,
            List<EventListResponse> responses,
            Map<Long, List<Participant>> participantsByEventId,
            User requester) {
        LocalDateTime startAt = occurrenceAt;
        LocalDateTime endAt = occurrenceAt.plusSeconds(durationSeconds);
        if (isOverlapped(startAt, endAt, dateRange)) {
            List<Participant> participants = participantsByEventId.getOrDefault(event.getEventId(), List.of());
            responses.add(EventListResponse.createOccurrence(
                    event,
                    rule,
                    occurrenceAt,
                    startAt,
                    endAt,
                    Participant.isParticipant(participants, requester),
                    participants));
        }
    }

    /**
     * 반복 예외 회차가 조회 조건을 충족하면 응답에 추가한다.
     *
     * <p>{@link RecurrenceExceptionType#CANCELLED} 회차는 제외하고,
     * {@link RecurrenceExceptionType#MODIFIED} 회차는 수정된 시간과 참여자 정보를 사용한다.</p>
     *
     * @param event 예외 회차의 원본 일정
     * @param rule 일정의 반복 규칙
     * @param recurrenceException 반영할 반복 예외
     * @param dateRange 회차를 조회할 날짜 범위
     * @param responses 회차를 추가할 응답 목록
     * @param participantsByExceptionId 반복 예외 ID별 참여자 목록
     * @param requester 일정을 조회하는 사용자
     * @param filterByParticipation 참여 회차만 포함할지 여부
     */
    private void addExceptionOccurrence(
            Event event,
            RecurrenceRule rule,
            RecurrenceException recurrenceException,
            DateRange dateRange,
            List<EventListResponse> responses,
            Map<Long, List<Participant>> participantsByExceptionId,
            User requester,
            boolean filterByParticipation) {
        if (recurrenceException.getExceptionType() == RecurrenceExceptionType.CANCELLED) {
            return;
        }

        List<Participant> participants =
                participantsByExceptionId.getOrDefault(recurrenceException.getRecurrenceExceptionId(), List.of());
        boolean isParticipant = Participant.isParticipant(participants, requester);
        if (filterByParticipation && !isParticipant) {
            return;
        }

        if (recurrenceException.getExceptionType() == RecurrenceExceptionType.MODIFIED
                && isOverlapped(
                        recurrenceException.getModifiedStartAt(), recurrenceException.getModifiedEndAt(), dateRange)) {
            responses.add(EventListResponse.createModifiedOccurrence(
                    event, rule, recurrenceException, isParticipant, participants));
        }
    }

    /**
     * 일정별 참여자 정보를 생성한다.
     *
     * @param events 참여자를 조회할 일정 목록
     * @return 일정 ID별 참여자 목록
     */
    private Map<Long, List<Participant>> createParticipantsByEventId(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        return eventParticipantRepository.findByEventInWithMember(events).stream()
                .collect(Collectors.groupingBy(
                        participant -> participant.getEvent().getEventId(),
                        Collectors.mapping(Participant::create, Collectors.toList())));
    }

    /**
     * 반복 예외별 참여자 정보를 생성한다.
     *
     * @param recurrenceExceptions 참여자를 조회할 반복 예외 목록
     * @return 반복 예외 ID별 참여자 목록
     */
    private Map<Long, List<Participant>> createParticipantsByExceptionId(
            List<RecurrenceException> recurrenceExceptions) {
        if (recurrenceExceptions.isEmpty()) {
            return Map.of();
        }

        return recurrenceExceptionParticipantRepository
                .findByRecurrenceExceptionInWithMember(recurrenceExceptions)
                .stream()
                .collect(Collectors.groupingBy(
                        participant -> participant.getRecurrenceException().getRecurrenceExceptionId(),
                        Collectors.mapping(Participant::create, Collectors.toList())));
    }

    /**
     * 반복 규칙이 조회 범위에 영향을 줄 수 있는지 확인한다.
     *
     * <p>종료 시각이 없거나 종료 시각이 조회 시작 시각보다 앞서지 않은 규칙만 확장 대상으로 판단한다.</p>
     *
     * @param rule 확인할 반복 규칙
     * @param dateRange 조회할 날짜 범위
     * @return 조회 범위에 영향을 줄 수 있으면 {@code true}, 그렇지 않으면 {@code false}
     */
    private boolean canAffectDateRange(RecurrenceRule rule, DateRange dateRange) {
        return rule.getUntilAt() == null || !rule.getUntilAt().isBefore(dateRange.startAt());
    }

    /**
     * 반복 종료 조건을 초과했는지 확인한다.
     *
     * <p>최대 발생 횟수와 종료 시각 중 먼저 충족되는 조건을 반복 종료 기준으로 사용한다.</p>
     *
     * @param rule 확인할 반복 규칙
     * @param occurrenceAt 현재 회차 시작 시각
     * @param generatedCount 현재까지 생성한 회차 수
     * @return 반복 종료 조건을 초과했으면 {@code true}, 그렇지 않으면 {@code false}
     */
    private boolean isAfterRecurrenceEnd(RecurrenceRule rule, LocalDateTime occurrenceAt, int generatedCount) {
        if (rule.getOccurrenceCount() != null && generatedCount > rule.getOccurrenceCount()) {
            return true;
        }

        return rule.getUntilAt() != null && occurrenceAt.isAfter(rule.getUntilAt());
    }

    /**
     * 현재 회차에서 다음 회차의 시작 시각을 계산한다.
     *
     * <p>일간·주간·월간·연간 빈도별 간격을 적용하며 주간과 월간 반복은 별도 보정 규칙을 사용한다.</p>
     *
     * @param occurrenceAt 현재 회차 시작 시각
     * @param rule 일정의 반복 규칙
     * @return 다음 회차 시작 시각
     */
    private LocalDateTime nextOccurrenceAt(LocalDateTime occurrenceAt, RecurrenceRule rule) {
        int interval = rule.getIntervalValue();
        RecurrenceFrequency frequency = rule.getFreq();

        return switch (frequency) {
            case DAILY -> occurrenceAt.plusDays(interval);
            case WEEKLY -> nextWeeklyOccurrenceAt(occurrenceAt, interval, rule);
            case MONTHLY -> nextMonthlyOccurrenceAt(occurrenceAt, interval, rule);
            case YEARLY -> occurrenceAt.plusYears(interval);
        };
    }

    /**
     * 반복 일정의 첫 회차 시작 시각을 계산한다.
     *
     * <p>시리즈 시작 시각이 있으면 우선 사용하고 주간 반복은 설정된 요일 중 첫 회차를 선택한다.</p>
     *
     * @param event 첫 회차를 계산할 일정
     * @param rule 일정의 반복 규칙
     * @return 첫 회차 시작 시각
     */
    private LocalDateTime firstOccurrenceAt(Event event, RecurrenceRule rule) {
        LocalDateTime seriesStartAt = rule.getSeriesStartAt() != null ? rule.getSeriesStartAt() : event.getStartAt();

        if (rule.getFreq() != RecurrenceFrequency.WEEKLY || sortedByDays(rule).isEmpty()) {
            return seriesStartAt;
        }

        return firstWeeklyOccurrenceAt(seriesStartAt, rule);
    }

    /**
     * 주간 반복의 다음 회차 시작 시각을 계산한다.
     *
     * <p>같은 주의 다음 반복 요일을 우선 선택하고 없으면 반복 간격을 적용한 주의 첫 반복 요일로 이동한다.</p>
     *
     * @param occurrenceAt 현재 회차 시작 시각
     * @param interval 반복 주기
     * @param rule 일정의 반복 규칙
     * @return 다음 주간 반복 회차 시작 시각
     */
    private LocalDateTime nextWeeklyOccurrenceAt(LocalDateTime occurrenceAt, int interval, RecurrenceRule rule) {
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
                        byDays.get(0)));
    }

    /**
     * 주간 반복의 첫 회차 시작 시각을 계산한다.
     *
     * @param seriesStartAt 반복 일정의 기준 시작 시각
     * @param rule 일정의 반복 규칙
     * @return 첫 주간 반복 회차 시작 시각
     */
    private LocalDateTime firstWeeklyOccurrenceAt(LocalDateTime seriesStartAt, RecurrenceRule rule) {
        return sortedByDays(rule).stream()
                .map(dayOfWeek -> moveToDayOfWeek(seriesStartAt, dayOfWeek))
                .min(Comparator.naturalOrder())
                .orElse(seriesStartAt);
    }

    /**
     * 반복 요일을 중복 없이 오름차순으로 정렬한다.
     *
     * @param rule 일정의 반복 규칙
     * @return 오름차순으로 정렬된 반복 요일 목록
     */
    private List<DayOfWeek> sortedByDays(RecurrenceRule rule) {
        return rule.getByDay().stream()
                .distinct()
                .sorted(Comparator.comparingInt(DayOfWeek::getValue))
                .toList();
    }

    /**
     * 지정 요일과 같거나 이후인 가장 가까운 시각으로 이동한다.
     *
     * @param dateTime 이동 기준 시각
     * @param dayOfWeek 이동할 요일
     * @return 지정 요일로 이동한 시각
     */
    private LocalDateTime moveToDayOfWeek(LocalDateTime dateTime, DayOfWeek dayOfWeek) {
        return dateTime.with(TemporalAdjusters.nextOrSame(dayOfWeek));
    }

    /**
     * 월간 반복의 다음 회차 시작 시각을 계산한다.
     *
     * <p>반복 일자가 대상 월에 없으면 해당 월의 마지막 날짜로 보정한다.</p>
     *
     * @param occurrenceAt 현재 회차 시작 시각
     * @param interval 반복 주기
     * @param rule 일정의 반복 규칙
     * @return 다음 월간 반복 회차 시작 시각
     */
    private LocalDateTime nextMonthlyOccurrenceAt(LocalDateTime occurrenceAt, int interval, RecurrenceRule rule) {
        LocalDateTime next = occurrenceAt.plusMonths(interval);
        if (rule.getByMonthDay() == null) {
            return next;
        }

        int dayOfMonth = Math.min(rule.getByMonthDay(), next.toLocalDate().lengthOfMonth());
        return next.withDayOfMonth(dayOfMonth);
    }

    /**
     * 일정 구간과 조회 범위가 겹치는지 확인한다.
     *
     * @param startAt 일정 구간 시작 시각
     * @param endAt 일정 구간 종료 시각
     * @param dateRange 조회할 날짜 범위
     * @return 두 구간이 겹치면 {@code true}, 그렇지 않으면 {@code false}
     */
    private boolean isOverlapped(LocalDateTime startAt, LocalDateTime endAt, DateRange dateRange) {
        if (startAt == null || endAt == null) {
            return false;
        }

        return startAt.isBefore(dateRange.endAt()) && endAt.isAfter(dateRange.startAt());
    }

    public record DateRange(LocalDateTime startAt, LocalDateTime endAt) {}

    public record OccurrenceKey(Long eventId, LocalDateTime originalOccurrenceAt) {}
}
