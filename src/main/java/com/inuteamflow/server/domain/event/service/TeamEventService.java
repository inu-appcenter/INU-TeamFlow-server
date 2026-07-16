package com.inuteamflow.server.domain.event.service;

import com.inuteamflow.server.domain.event.dto.request.TeamEventCreateRequest;
import com.inuteamflow.server.domain.event.dto.request.TeamEventUpdateRequest;
import com.inuteamflow.server.domain.event.dto.response.EventDetailResponse;
import com.inuteamflow.server.domain.event.dto.response.EventListResponse;
import com.inuteamflow.server.domain.event.entity.*;
import com.inuteamflow.server.domain.event.enums.EventRole;
import com.inuteamflow.server.domain.event.enums.RecurrenceEditScope;
import com.inuteamflow.server.domain.event.repository.EventParticipantRepository;
import com.inuteamflow.server.domain.event.repository.EventRepository;
import com.inuteamflow.server.domain.event.repository.RecurrenceExceptionParticipantRepository;
import com.inuteamflow.server.domain.event.repository.RecurrenceExceptionRepository;
import com.inuteamflow.server.domain.notification.enums.NotificationType;
import com.inuteamflow.server.domain.notification.service.NotificationService;
import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.domain.team.enums.TeamRole;
import com.inuteamflow.server.domain.team.repository.TeamMemberRepository;
import com.inuteamflow.server.domain.team.repository.TeamRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamEventService {

    private final EventOccurrenceService eventOccurrenceService;
    private final EventRecurrenceService eventRecurrenceService;
    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final RecurrenceExceptionRepository recurrenceExceptionRepository;
    private final RecurrenceExceptionParticipantRepository recurrenceExceptionParticipantRepository;

    private final NotificationService notificationService;

    public List<EventListResponse> getTeamEventList(
            User user,
            Long teamId,
            Integer year,
            Integer month
    ) {
        Team team = getTeam(teamId);
        validateTeamMember(team, user);
        EventOccurrenceService.DateRange dateRange = eventOccurrenceService.createMonthlyDateRange(year, month);

        List<Event> singleEvents = eventRepository.findByTeamAndIsSingleAndStartAtBeforeAndEndAtAfter(
                team,
                true,
                dateRange.endAt(),
                dateRange.startAt()
        );
        List<Event> recurringEvents = eventRepository.findByTeamAndIsSingleAndStartAtBefore(
                team,
                false,
                dateRange.endAt()
        );
        List<EventListResponse> recurringOccurrences = eventOccurrenceService.expandRecurringEvents(
                recurringEvents,
                dateRange
        );

        return eventOccurrenceService.mergeAndSort(singleEvents, recurringOccurrences);
    }

    @Transactional
    public EventDetailResponse createTeamEvent(
            User user,
            Long teamId,
            TeamEventCreateRequest request
    ) {
        Team team = getTeam(teamId);
        TeamMember host = validateTeamEventManager(team, user);
        Event event = eventRepository.save(Event.create(team, request));
        RecurrenceRule recurrenceRule = eventRecurrenceService.createRecurrenceRule(event, request);
        createParticipants(event, team, host, request.getParticipants());

        List<User> receivers = eventParticipantRepository.findUsersByEventExcluding(event, user.getUserId());

        notificationService.createNotifications(
                receivers,
                "[" + team.getName() + "] 팀에 새 일정이 추가됐어요",
                "'" + event.getTitle() + "' 일정을 확인해보세요",
                NotificationType.TEAM_SCHEDULE,
                "/team/"+team.getTeamId()
        );

        return EventDetailResponse.create(event, recurrenceRule, team.getName());
    }

    @Transactional
    public EventDetailResponse updateTeamEvent(
            User user,
            Long teamId,
            Long eventId,
            TeamEventUpdateRequest request
    ) {
        Event event = getTeamEvent(teamId, eventId);
        Team team = getTeam(teamId);
        validateTeamEventManager(team, user);

        boolean isRecurring = !Boolean.TRUE.equals(event.getIsSingle());
        boolean isThisInstance = isRecurring && request.getRecurrenceEditScope() == RecurrenceEditScope.THIS_INSTANCE;
        boolean isThisAndFollowing = isRecurring && request.getRecurrenceEditScope() == RecurrenceEditScope.THIS_AND_FOLLOWING;

        EventDetailResponse response = eventRecurrenceService.updateEvent(event, team, request);

        List<User> receivers;
        if (isThisInstance) {
            RecurrenceException recurrenceException = recurrenceExceptionRepository
                    .findByEventAndOriginalOccurrenceAt(event, request.getOccurrenceAt())
                    .orElseThrow(() -> new RestApiException(CustomErrorCode.EVENT_RECURRENCE_OCCURRENCE_NOT_FOUND));
            syncExceptionParticipants(recurrenceException, team, request.getParticipants());
            receivers = recurrenceExceptionParticipantRepository.findUsersByExceptionExcluding(recurrenceException, user.getUserId());
        } else if (isThisAndFollowing) {
            Event followingEvent = getTeamEvent(teamId, response.getEventId());
            syncParticipants(followingEvent, team, request.getParticipants());
            receivers = eventParticipantRepository.findUsersByEventExcluding(followingEvent, user.getUserId());
        } else {
            syncParticipants(event, team, request.getParticipants());
            receivers = eventParticipantRepository.findUsersByEventExcluding(event, user.getUserId());
        }

        String updateContent;
        if (isThisInstance) {
            updateContent = "'" + event.getTitle() + "' 이번 회차 일정이 변경됐어요";
        } else if (isThisAndFollowing) {
            updateContent = "'" + event.getTitle() + "' 이후 일정이 변경됐어요";
        } else {
            updateContent = "'" + event.getTitle() + "' 일정이 변경됐어요";
        }
        notificationService.createNotifications(
                receivers,
                "[" + event.getTeam().getName() + "] 팀의 일정이 변경됐어요",
                updateContent,
                NotificationType.TEAM_SCHEDULE,
                "/team/" + team.getTeamId()
        );

        return response;
    }

    @Transactional
    public void deleteTeamEvent(
            User user,
            Long teamId,
            Long eventId,
            RecurrenceEditScope recurrenceEditScope,
            LocalDateTime occurrenceAt
    ) {
        Event event = getTeamEvent(teamId, eventId);
        Team team = getTeam(teamId);
        validateTeamEventManager(team, user);

        List<User> receivers;
        // RecurrenceException이 이미 존재하면 RecurrenceExceptionParticipant 기준으로 조회한다.
        // 없으면 EventParticipant 기준으로 조회한다.
        if (recurrenceEditScope == RecurrenceEditScope.THIS_INSTANCE) {
            receivers = recurrenceExceptionRepository
                    .findByEventAndOriginalOccurrenceAt(event, occurrenceAt)
                    .map(re -> recurrenceExceptionParticipantRepository.findUsersByExceptionExcluding(re, user.getUserId()))
                    .orElseGet(() -> eventParticipantRepository.findUsersByEventExcluding(event, user.getUserId()));
        } else {
            receivers = eventParticipantRepository.findUsersByEventExcluding(event, user.getUserId());
        }

        notificationService.createNotifications(
                receivers,
                "'" + event.getTitle() + "' 일정이 삭제됐어요",
                user.getName() + "님이 일정을 삭제했어요",
                NotificationType.TEAM_SCHEDULE,
                "/team/" + team.getTeamId()
        );

        if (eventRecurrenceService.deleteEvent(event, recurrenceEditScope, occurrenceAt)) {
            eventParticipantRepository.deleteByEvent(event);
            eventRepository.delete(event);
        }
    }

    private void createParticipants(
            Event event,
            Team team,
            TeamMember host,
            List<Long> participantIds
    ) {
        List<EventParticipant> participants = new ArrayList<>();
        participants.add(EventParticipant.create(event, host, EventRole.HOST));

        if (participantIds != null && !participantIds.isEmpty()) {
            List<Long> validIds = participantIds.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .filter(id -> !id.equals(host.getTeamMemberId()))
                    .toList();

            if (!validIds.isEmpty()) {
                Map<Long, TeamMember> memberMap = teamMemberRepository.findByTeamAndIds(team, validIds)
                        .stream()
                        .collect(Collectors.toMap(TeamMember::getTeamMemberId, Function.identity()));

                for (Long id : validIds) {
                    if (!memberMap.containsKey(id)) {
                        throw new RestApiException(CustomErrorCode.EVENT_PARTICIPANT_INVALID);
                    }
                    participants.add(EventParticipant.create(event, memberMap.get(id), EventRole.PARTICIPANT));
                }
            }
        }

        eventParticipantRepository.saveAll(participants);
    }

    private void createExceptionParticipants(
            RecurrenceException recurrenceException,
            Team team,
            TeamMember host,
            List<Long> participantIds
    ) {
        List<RecurrenceExceptionParticipant> participants = new ArrayList<>();
        participants.add(RecurrenceExceptionParticipant.create(recurrenceException, host, EventRole.HOST));

        if (participantIds != null && !participantIds.isEmpty()) {
            List<Long> validIds = participantIds.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .filter(id -> !id.equals(host.getTeamMemberId()))
                    .toList();

            if (!validIds.isEmpty()) {
                Map<Long, TeamMember> memberMap = teamMemberRepository.findByTeamAndIds(team, validIds)
                        .stream()
                        .collect(Collectors.toMap(TeamMember::getTeamMemberId, Function.identity()));

                for (Long id : validIds) {
                    if (!memberMap.containsKey(id)) {
                        throw new RestApiException(CustomErrorCode.EVENT_PARTICIPANT_INVALID);
                    }
                    participants.add(RecurrenceExceptionParticipant.create(
                            recurrenceException, memberMap.get(id), EventRole.PARTICIPANT));
                }
            }
        }

        recurrenceExceptionParticipantRepository.saveAll(participants);
    }

    private void syncParticipants(
            Event event,
            Team team,
            List<Long> participantIds
    ) {
        Event targetEvent = getTeamEvent(team.getTeamId(), event.getEventId());
        TeamMember host = eventParticipantRepository.findByEvent(event).stream()
                .filter(participant -> participant.getEventRole() == EventRole.HOST)
                .findFirst()
                .map(EventParticipant::getTeamMember)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.EVENT_PARTICIPANT_HOST_NOT_FOUND));

        eventParticipantRepository.deleteByEvent(event);
        eventParticipantRepository.flush();

        createParticipants(targetEvent, team, host, participantIds);
    }

    private void syncExceptionParticipants(
            RecurrenceException recurrenceException,
            Team team,
            List<Long> participantIds
    ) {
        TeamMember host = eventParticipantRepository.findByEvent(recurrenceException.getEvent()).stream()
                .filter(participant -> participant.getEventRole() == EventRole.HOST)
                .findFirst()
                .map(EventParticipant::getTeamMember)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.EVENT_PARTICIPANT_HOST_NOT_FOUND));

        recurrenceExceptionParticipantRepository.deleteByRecurrenceException(recurrenceException);
        recurrenceExceptionParticipantRepository.flush();

        createExceptionParticipants(recurrenceException, team, host, participantIds);
    }

    private Event getTeamEvent(
            Long teamId,
            Long eventId
    ) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.EVENT_NOT_FOUND));

        if (!teamId.equals(event.getTeamId())) {
            throw new RestApiException(CustomErrorCode.EVENT_TEAM_MISMATCH);
        }

        return event;
    }

    private Team getTeam(
            Long teamId
    ) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));
    }

    private TeamMember validateTeamMember(
            Team team,
            User user
    ) {
        return teamMemberRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));
    }

    private TeamMember validateTeamEventManager(
            Team team,
            User user
    ) {
        TeamMember teamMember = validateTeamMember(team, user);
        if (teamMember.getTeamRole() == TeamRole.LEADER
                || teamMember.getTeamRole() == TeamRole.MANAGER) {
            return teamMember;
        }

        throw new RestApiException(CustomErrorCode.EVENT_FORBIDDEN);
    }
}
