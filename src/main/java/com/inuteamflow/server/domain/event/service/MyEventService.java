package com.inuteamflow.server.domain.event.service;

import com.inuteamflow.server.domain.event.dto.request.MyEventCreateRequest;
import com.inuteamflow.server.domain.event.dto.request.MyEventUpdateRequest;
import com.inuteamflow.server.domain.event.dto.response.EventDetailResponse;
import com.inuteamflow.server.domain.event.dto.response.EventListResponse;
import com.inuteamflow.server.domain.event.entity.Event;
import com.inuteamflow.server.domain.event.entity.RecurrenceRule;
import com.inuteamflow.server.domain.event.enums.RecurrenceEditScope;
import com.inuteamflow.server.domain.event.repository.EventParticipantRepository;
import com.inuteamflow.server.domain.event.repository.EventRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyEventService {

    private final EventOccurrenceService eventOccurrenceService;
    private final EventRecurrenceService eventRecurrenceService;
    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;

    public List<EventListResponse> getMyEventList(
            User user,
            Integer year,
            Integer month
    ) {
        EventOccurrenceService.DateRange dateRange = eventOccurrenceService.createMonthlyDateRange(year, month);

        List<Event> singleEvents = new ArrayList<>(eventRepository.findByCreatedByAndTeamIsNullAndIsSingleAndStartAtBeforeAndEndAtAfter(
                user.getUserId(),
                true,
                dateRange.endAt(),
                dateRange.startAt()
        ));
        List<Event> recurringEvents = eventRepository.findByCreatedByAndTeamIsNullAndIsSingleAndStartAtBefore(
                user.getUserId(),
                false,
                dateRange.endAt()
        );
        List<EventListResponse> recurringOccurrences = new ArrayList<>(eventOccurrenceService.expandRecurringEvents(
                recurringEvents,
                dateRange,
                user
        ));

        List<Long> participatingEventIds = eventParticipantRepository.findEventIdsByUser(user)
                .stream()
                .distinct()
                .toList();

        if (!participatingEventIds.isEmpty()) {
            singleEvents.addAll(eventRepository.findByEventIdInAndIsSingleAndStartAtBeforeAndEndAtAfter(
                    participatingEventIds,
                    true,
                    dateRange.endAt(),
                    dateRange.startAt()
            ));

            List<Event> participatingRecurringEvents = eventRepository.findByEventIdInAndIsSingleAndStartAtBefore(
                    participatingEventIds,
                    false,
                    dateRange.endAt()
            );
            recurringOccurrences.addAll(eventOccurrenceService.expandRecurringEvents(
                    participatingRecurringEvents,
                    dateRange,
                    user,
                    true
            ));
        }

        return eventOccurrenceService.mergeAndSort(singleEvents, recurringOccurrences, user);
    }

    @Transactional
    public EventDetailResponse createMyEvent(
            User user,
            MyEventCreateRequest request
    ) {
        Event event = eventRepository.save(Event.create(request));
        RecurrenceRule recurrenceRule = eventRecurrenceService.createRecurrenceRule(event, request);

        return EventDetailResponse.create(event, recurrenceRule, null, false, List.of());
    }

    @Transactional
    public EventDetailResponse updateMyEvent(
            User user,
            Long eventId,
            MyEventUpdateRequest request
    ) {
        Event event = getMyEvent(user, eventId);
        EventRecurrenceService.EventUpdateResult updateResult =
                eventRecurrenceService.updateEvent(event, null, request);

        if (updateResult.recurrenceException() != null) {
            return EventDetailResponse.createModifiedOccurrence(
                    updateResult.event(),
                    updateResult.recurrenceRule(),
                    updateResult.recurrenceException(),
                    null,
                    false,
                    List.of()
            );
        }

        return EventDetailResponse.create(
                updateResult.event(),
                updateResult.recurrenceRule(),
                null,
                false,
                List.of()
        );
    }

    @Transactional
    public void deleteMyEvent(
            User user,
            Long eventId,
            RecurrenceEditScope recurrenceEditScope,
            LocalDateTime occurrenceAt
    ) {
        Event event = getMyEvent(user, eventId);

        if (eventRecurrenceService.deleteEvent(event, recurrenceEditScope, occurrenceAt)) {
            eventRepository.delete(event);
        }
    }

    private Event getMyEvent(
            User user,
            Long eventId
    ) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.EVENT_NOT_FOUND));

        if (event.getTeamId() != null || !event.getCreatedBy().equals(user.getUserId())) {
            throw new RestApiException(CustomErrorCode.EVENT_FORBIDDEN);
        }

        return event;
    }
}
