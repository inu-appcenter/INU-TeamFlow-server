package com.inuteamflow.server.domain.event.service;

import com.inuteamflow.server.domain.event.dto.request.MyEventCreateRequest;
import com.inuteamflow.server.domain.event.dto.request.MyEventUpdateRequest;
import com.inuteamflow.server.domain.event.dto.response.EventDetailResponse;
import com.inuteamflow.server.domain.event.dto.response.EventListResponse;
import com.inuteamflow.server.domain.event.entity.Event;
import com.inuteamflow.server.domain.event.entity.RecurrenceRule;
import com.inuteamflow.server.domain.event.enums.EventColor;
import com.inuteamflow.server.domain.event.enums.RecurrenceEditScope;
import com.inuteamflow.server.domain.event.repository.EventParticipantRepository;
import com.inuteamflow.server.domain.event.repository.EventRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyEventService {

    private final EventOccurrenceService eventOccurrenceService;
    private final EventRecurrenceService eventRecurrenceService;
    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 사용자가 생성하거나 참여하는 월간 일정을 조회한다.
     *
     * <p>단일 일정과 반복 일정의 발생 회차를 병합하여 시작 시간 오름차순으로 반환한다.</p>
     *
     * @param user 일정을 조회하는 사용자
     * @param year 조회할 연도
     * @param month 조회할 월
     * @return 시작 시간 오름차순으로 정렬된 일정 목록
     * @throws RestApiException 월이 유효하지 않은 경우
     */
    public List<EventListResponse> getMyEventList(User user, Integer year, Integer month) {
        EventOccurrenceService.DateRange dateRange = eventOccurrenceService.createMonthlyDateRange(year, month);

        List<Event> singleEvents =
                new ArrayList<>(eventRepository.findByCreatedByAndTeamIsNullAndIsSingleAndStartAtBeforeAndEndAtAfter(
                        user.getUserId(), true, dateRange.endAt(), dateRange.startAt()));
        List<Event> recurringEvents = eventRepository.findByCreatedByAndTeamIsNullAndIsSingleAndStartAtBefore(
                user.getUserId(), false, dateRange.endAt());
        List<EventListResponse> recurringOccurrences =
                new ArrayList<>(eventOccurrenceService.expandRecurringEvents(recurringEvents, dateRange, user));

        List<Long> participatingEventIds = eventParticipantRepository.findEventIdsByUser(user).stream()
                .distinct()
                .toList();

        if (!participatingEventIds.isEmpty()) {
            singleEvents.addAll(eventRepository.findByEventIdInAndIsSingleAndStartAtBeforeAndEndAtAfter(
                    participatingEventIds, true, dateRange.endAt(), dateRange.startAt()));

            List<Event> participatingRecurringEvents = eventRepository.findByEventIdInAndIsSingleAndStartAtBefore(
                    participatingEventIds, false, dateRange.endAt());
            recurringOccurrences.addAll(
                    eventOccurrenceService.expandRecurringEvents(participatingRecurringEvents, dateRange, user, true));
        }

        return eventOccurrenceService.mergeAndSort(singleEvents, recurringOccurrences, user);
    }

    /**
     * 개인 일정을 생성한다.
     *
     * <p>반복 설정이 포함된 경우 일정과 함께 반복 규칙을 생성한다.</p>
     *
     * @param user 일정을 생성하는 사용자
     * @param request 개인 일정 생성 요청
     * @return 생성된 일정 상세 정보
     */
    @Transactional
    public EventDetailResponse createMyEvent(User user, MyEventCreateRequest request) {
        Event event = eventRepository.save(Event.create(request));
        RecurrenceRule recurrenceRule = eventRecurrenceService.createRecurrenceRule(event, request);

        return EventDetailResponse.of(event, recurrenceRule, null, false, List.of());
    }

    /**
     * 신규 가입자에게 기본 안내 일정을 생성한다.
     *
     * <p>가입 요청은 인증 전이라 JPA Auditing이 작성자를 채우지 못하므로, 일정마다 작성자를 직접 지정한 뒤 바로 저장한다.</p>
     *
     * @param user 가입한 사용자
     */
    @Transactional
    public void createWelcomeEvents(User user) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        List<Event> welcomeEvents = List.of(
                Event.createAllDay("안녕하세요!", today, EventColor.SUN),
                Event.createAllDay("모이미에 온 걸 환영해요", today, EventColor.SUN),
                Event.createAllDay("일정을 추가해보세요", today.plusDays(1), EventColor.SUN));

        for (Event event : welcomeEvents) {
            event.assignAuditor(user.getUserId());
            eventRepository.save(event);
        }
    }

    /**
     * 개인 일정을 수정한다.
     *
     * <p>반복 일정의 특정 회차를 수정하면 예외 회차 정보를 포함하여 반환한다.</p>
     *
     * @param user 일정을 수정하는 사용자
     * @param eventId 수정할 일정 ID
     * @param request 개인 일정 수정 요청
     * @return 수정된 일정 상세 정보
     * @throws RestApiException 일정을 찾을 수 없거나 수정 권한이 없는 경우,
     *                          또는 반복 일정이나 회차 정보가 유효하지 않은 경우
     */
    @Transactional
    public EventDetailResponse updateMyEvent(User user, Long eventId, MyEventUpdateRequest request) {
        Event event = getMyEvent(user, eventId);
        EventRecurrenceService.EventUpdateResult updateResult =
                eventRecurrenceService.updateEvent(event, null, request);

        if (updateResult.recurrenceException() != null) {
            return EventDetailResponse.of(
                    updateResult.event(),
                    updateResult.recurrenceRule(),
                    updateResult.recurrenceException(),
                    null,
                    false,
                    List.of());
        }

        return EventDetailResponse.of(updateResult.event(), updateResult.recurrenceRule(), null, false, List.of());
    }

    /**
     * 개인 일정을 삭제한다.
     *
     * <p>반복 일정은 요청된 편집 범위에 따라 특정 회차, 이후 회차 또는 전체 일정을 삭제한다.</p>
     *
     * @param user 일정을 삭제하는 사용자
     * @param eventId 삭제할 일정 ID
     * @param recurrenceEditScope 반복 일정 편집 범위
     * @param occurrenceAt 편집 대상 회차의 원래 시작 시각
     * @throws RestApiException 일정을 찾을 수 없거나 삭제 권한이 없는 경우,
     *                          또는 반복 일정이나 회차 정보가 유효하지 않은 경우
     */
    @Transactional
    public void deleteMyEvent(
            User user, Long eventId, RecurrenceEditScope recurrenceEditScope, LocalDateTime occurrenceAt) {
        Event event = getMyEvent(user, eventId);

        if (eventRecurrenceService.deleteEvent(event, recurrenceEditScope, occurrenceAt)) {
            eventRepository.delete(event);
        }
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 사용자가 소유한 개인 일정을 조회한다.
     *
     * <p>팀 일정이 아니고 일정 생성자 ID가 요청 사용자 ID와 일치하는 경우에만 반환한다.</p>
     *
     * @param user 일정 소유자
     * @param eventId 조회할 일정 ID
     * @return 조회된 개인 일정
     * @throws RestApiException 일정을 찾을 수 없거나 사용자에게 접근 권한이 없는 경우
     */
    private Event getMyEvent(User user, Long eventId) {
        Event event = eventRepository
                .findById(eventId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.EVENT_NOT_FOUND));

        if (event.getTeamId() != null || !event.getCreatedBy().equals(user.getUserId())) {
            throw new RestApiException(CustomErrorCode.EVENT_FORBIDDEN);
        }

        return event;
    }
}
