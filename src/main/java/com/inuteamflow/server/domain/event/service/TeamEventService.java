package com.inuteamflow.server.domain.event.service;

import com.inuteamflow.server.domain.event.dto.Participant;
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
                dateRange,
                user
        );

        return eventOccurrenceService.mergeAndSort(singleEvents, recurringOccurrences, user);
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

        // 참여자 정보를 제공하기 위한 전용 DTO로 변환
        List<Participant> participants = createParticipants(event, team, host, request.getParticipants()).stream()
                .map(Participant::create)
                .toList();

        List<User> receivers = eventParticipantRepository.findUsersByEventExcluding(event, user.getUserId());

        notificationService.createNotifications(
                receivers,
                "[" + team.getName() + "] 팀에 새 일정이 추가됐어요",
                "'" + event.getTitle() + "' 일정을 확인해보세요",
                NotificationType.TEAM_SCHEDULE,
                "/team/"+team.getTeamId()
        );

        return EventDetailResponse.create(event, recurrenceRule, team.getName(), true ,participants);
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

        EventRecurrenceService.EventUpdateResult updateResult =
                eventRecurrenceService.updateEvent(event, team, request);
        boolean isThisInstance = updateResult.editScope() == RecurrenceEditScope.THIS_INSTANCE;
        boolean isThisAndFollowing = updateResult.editScope() == RecurrenceEditScope.THIS_AND_FOLLOWING;

        TeamMember followingEventHost = isThisAndFollowing
                ? findEventHost(event)
                : null;

        List<User> receivers;
        List<Participant> participants;

        if (isThisInstance) {
            RecurrenceException recurrenceException = updateResult.recurrenceException();
            participants = syncExceptionParticipants(recurrenceException, team, request.getParticipants());
            receivers = recurrenceExceptionParticipantRepository.findUsersByExceptionExcluding(recurrenceException, user.getUserId());
        } else if (isThisAndFollowing) {
            Event followingEvent = updateResult.event();
            participants = createParticipants(followingEvent, team, followingEventHost, request.getParticipants()).stream()
                    .map(Participant::create)
                    .toList();
            receivers = eventParticipantRepository.findUsersByEventExcluding(followingEvent, user.getUserId());
        } else {
            Event updatedEvent = updateResult.event();
            participants = syncParticipants(updatedEvent, team, request.getParticipants());
            receivers = eventParticipantRepository.findUsersByEventExcluding(updatedEvent, user.getUserId());
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

        boolean isParticipant = Participant.isParticipant(participants, user);
        if (isThisInstance) {
            return EventDetailResponse.createModifiedOccurrence(
                    updateResult.event(),
                    updateResult.recurrenceRule(),
                    updateResult.recurrenceException(),
                    team.getName(),
                    isParticipant,
                    participants
            );
        }

        return EventDetailResponse.create(
                updateResult.event(),
                updateResult.recurrenceRule(),
                team.getName(),
                isParticipant,
                participants
        );
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

    private List<EventParticipant> createParticipants(
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

        return eventParticipantRepository.saveAll(participants);
    }

    private List<RecurrenceExceptionParticipant> createExceptionParticipants(
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

        return recurrenceExceptionParticipantRepository.saveAll(participants);
    }

    private List<Participant> syncParticipants(
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

        return createParticipants(targetEvent, team, host, participantIds).stream()
                .map(Participant::create)
                .toList();
    }

    private List<Participant> syncExceptionParticipants(
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

        return createExceptionParticipants(recurrenceException, team, host, participantIds).stream()
                .map(Participant::create)
                .toList();
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

    private TeamMember findEventHost(
            Event event
    ) {
        return eventParticipantRepository.findByEvent(event).stream()
                .filter(participant -> participant.getEventRole() == EventRole.HOST)
                .findFirst()
                .map(EventParticipant::getTeamMember)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.EVENT_PARTICIPANT_HOST_NOT_FOUND                ));
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
