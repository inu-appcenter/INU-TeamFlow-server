package com.inuteamflow.server.domain.event.service;

import com.inuteamflow.server.domain.event.dto.EventCreateCommand;
import com.inuteamflow.server.domain.event.dto.EventUpdateCommand;
import com.inuteamflow.server.domain.event.dto.response.EventDetailResponse;
import com.inuteamflow.server.domain.event.entity.Event;
import com.inuteamflow.server.domain.event.entity.EventParticipant;
import com.inuteamflow.server.domain.event.entity.RecurrenceException;
import com.inuteamflow.server.domain.event.entity.RecurrenceRule;
import com.inuteamflow.server.domain.event.enums.RecurrenceEditScope;
import com.inuteamflow.server.domain.event.repository.EventParticipantRepository;
import com.inuteamflow.server.domain.event.repository.EventRepository;
import com.inuteamflow.server.domain.event.repository.RecurrenceExceptionRepository;
import com.inuteamflow.server.domain.event.repository.RecurrenceRuleRepository;
import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventRecurrenceService {

    private final EventOccurrenceService eventOccurrenceService;
    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final RecurrenceRuleRepository recurrenceRuleRepository;
    private final RecurrenceExceptionRepository recurrenceExceptionRepository;

    public RecurrenceRule createRecurrenceRule(
            Event event,
            EventCreateCommand command
    ) {
        if (command.getRecurrence() == null) {
            return null;
        }

        return recurrenceRuleRepository.save(RecurrenceRule.create(
                event,
                command.getRecurrence(),
                event.getStartAt()
        ));
    }

    public RecurrenceRule createRecurrenceRule(
            Event event,
            EventUpdateCommand command
    ) {
        if (command.getRecurrence() == null) {
            return null;
        }

        return recurrenceRuleRepository.save(RecurrenceRule.create(
                event,
                command.getRecurrence(),
                event.getStartAt()
        ));
    }

    public EventDetailResponse updateEvent(
            Event event,
            Team team,
            EventUpdateCommand command
    ) {
        String teamName = team == null ? null : team.getName();
        event.increaseSequence();

        if (Boolean.TRUE.equals(event.getIsSingle())) {
            return updateSingleEvent(event, command, teamName);
        }
        validateRecurrenceRequired(command);

        RecurrenceEditScope editScope = command.getRecurrenceEditScope() == null
                ? RecurrenceEditScope.ALL_SERIES
                : command.getRecurrenceEditScope();

        if ((editScope == RecurrenceEditScope.THIS_INSTANCE || editScope == RecurrenceEditScope.THIS_AND_FOLLOWING)
                && command.getOccurrenceAt() == null) {
            throw new RestApiException(CustomErrorCode.EVENT_RECURRENCE_OCCURRENCE_REQUIRED);
        }

        return switch (editScope) {
            case ALL_SERIES -> {
                RecurrenceRule recurrenceRule = updateAllSeries(event, command);
                yield EventDetailResponse.create(event, recurrenceRule, teamName);
            }
            case THIS_INSTANCE -> {
                RecurrenceRule recurrenceRule = getRecurrenceRule(event);
                RecurrenceException recurrenceException = updateThisInstance(event, command);
                yield EventDetailResponse.createModifiedOccurrence(event, recurrenceRule, recurrenceException, teamName);
            }
            case THIS_AND_FOLLOWING -> {
                FollowingSeries followingSeries = updateThisAndFollowing(event, team, command);
                yield EventDetailResponse.create(followingSeries.event(), followingSeries.recurrenceRule(), teamName);
            }
        };
    }

    private EventDetailResponse updateSingleEvent(
            Event event,
            EventUpdateCommand command,
            String teamName
    ) {
        event.update(command);
        if (command.getRecurrence() == null) {
            return EventDetailResponse.create(event, null, teamName);
        }

        event.changeToRecurring();
        RecurrenceRule recurrenceRule = createRecurrenceRule(event, command);

        return EventDetailResponse.create(event, recurrenceRule, teamName);
    }

    private RecurrenceRule updateAllSeries(
            Event event,
            EventUpdateCommand command
    ) {
        RecurrenceRule recurrenceRule = getRecurrenceRule(event);

        event.update(command);
        recurrenceRule.update(command.getRecurrence(), event.getStartAt());
        recurrenceExceptionRepository.deleteByEvent_EventId(event.getEventId());

        return recurrenceRule;
    }

    private RecurrenceException updateThisInstance(
            Event event,
            EventUpdateCommand command
    ) {
        RecurrenceRule recurrenceRule = getRecurrenceRule(event);
        validateOccurrence(event, recurrenceRule, command.getOccurrenceAt());

        RecurrenceException recurrenceException = recurrenceExceptionRepository
                .findByEvent_EventIdAndOriginalOccurrenceAt(event.getEventId(), command.getOccurrenceAt())
                .orElseGet(() -> recurrenceExceptionRepository.save(
                        RecurrenceException.createModified(event, command)
                ));
        recurrenceException.update(command);

        return recurrenceException;
    }

    private FollowingSeries updateThisAndFollowing(
            Event event,
            Team team,
            EventUpdateCommand command
    ) {
        RecurrenceRule recurrenceRule = getRecurrenceRule(event);
        validateOccurrence(event, recurrenceRule, command.getOccurrenceAt());

        recurrenceRule.finishBefore(command.getOccurrenceAt());
        recurrenceExceptionRepository.deleteByEvent_EventIdAndOriginalOccurrenceAtGreaterThanEqual(
                event.getEventId(),
                command.getOccurrenceAt()
        );

        Event followingEvent = team == null
                ? eventRepository.save(Event.createRecurring(command))
                : eventRepository.save(Event.createRecurring(team, command));
        copyParticipants(event, followingEvent);
        RecurrenceRule followingRule = recurrenceRuleRepository.save(RecurrenceRule.create(
                followingEvent,
                command.getRecurrence(),
                followingEvent.getStartAt()
        ));

        return new FollowingSeries(followingEvent, followingRule);
    }

    private void copyParticipants(
            Event originalEvent,
            Event followingEvent
    ) {
        if (followingEvent.getTeamId() == null) {
            return;
        }

        List<EventParticipant> copiedParticipants = eventParticipantRepository
                .findByEvent_EventId(originalEvent.getEventId())
                .stream()
                .map(participant -> EventParticipant.create(
                        followingEvent,
                        participant.getTeamMember(),
                        participant.getEventRole()
                ))
                .toList();

        eventParticipantRepository.saveAll(copiedParticipants);
    }

    public boolean deleteEvent(
            Event event,
            RecurrenceEditScope recurrenceEditScope,
            LocalDateTime occurrenceAt
    ) {
        if (Boolean.TRUE.equals(event.getIsSingle())) {
            return true;
        }

        RecurrenceEditScope editScope = recurrenceEditScope == null
                ? RecurrenceEditScope.ALL_SERIES
                : recurrenceEditScope;

        if ((editScope == RecurrenceEditScope.THIS_INSTANCE || editScope == RecurrenceEditScope.THIS_AND_FOLLOWING)
                && occurrenceAt == null) {
            throw new RestApiException(CustomErrorCode.EVENT_RECURRENCE_OCCURRENCE_REQUIRED);
        }

        switch (editScope) {
            case ALL_SERIES -> {
                deleteAllSeries(event);
                return true;
            }
            case THIS_INSTANCE -> deleteThisInstance(event, occurrenceAt);
            case THIS_AND_FOLLOWING -> deleteThisAndFollowing(event, occurrenceAt);
        }

        return false;
    }

    private void deleteAllSeries(
            Event event
    ) {
        recurrenceExceptionRepository.deleteByEvent_EventId(event.getEventId());
        recurrenceRuleRepository.deleteByEvent_EventId(event.getEventId());
    }

    private void deleteThisInstance(
            Event event,
            LocalDateTime occurrenceAt
    ) {
        RecurrenceRule recurrenceRule = getRecurrenceRule(event);
        validateOccurrence(event, recurrenceRule, occurrenceAt);

        RecurrenceException recurrenceException = recurrenceExceptionRepository
                .findByEvent_EventIdAndOriginalOccurrenceAt(event.getEventId(), occurrenceAt)
                .orElseGet(() -> recurrenceExceptionRepository.save(
                        RecurrenceException.createCancelled(event, occurrenceAt)
                ));
        recurrenceException.cancel();
    }

    private void deleteThisAndFollowing(
            Event event,
            LocalDateTime occurrenceAt
    ) {
        RecurrenceRule recurrenceRule = getRecurrenceRule(event);
        validateOccurrence(event, recurrenceRule, occurrenceAt);

        recurrenceRule.finishBefore(occurrenceAt);
        recurrenceExceptionRepository.deleteByEvent_EventIdAndOriginalOccurrenceAtGreaterThanEqual(
                event.getEventId(),
                occurrenceAt
        );
    }

    private RecurrenceRule getRecurrenceRule(
            Event event
    ) {
        return recurrenceRuleRepository.findByEvent_EventId(event.getEventId())
                .orElseThrow(() -> new RestApiException(CustomErrorCode.EVENT_RECURRENCE_RULE_NOT_FOUND));
    }

    private void validateRecurrenceRequired(
            EventUpdateCommand command
    ) {
        if (command.getRecurrence() == null) {
            throw new RestApiException(CustomErrorCode.EVENT_RECURRENCE_REQUIRED);
        }
    }

    private void validateOccurrence(
            Event event,
            RecurrenceRule recurrenceRule,
            LocalDateTime occurrenceAt
    ) {
        if (!eventOccurrenceService.existsOccurrence(event, recurrenceRule, occurrenceAt)) {
            throw new RestApiException(CustomErrorCode.EVENT_RECURRENCE_OCCURRENCE_NOT_FOUND);
        }
    }

    private record FollowingSeries(
            Event event,
            RecurrenceRule recurrenceRule
    ) {
    }
}
