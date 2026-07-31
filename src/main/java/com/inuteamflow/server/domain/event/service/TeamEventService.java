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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 팀의 월간 일정을 조회한다.
     *
     * <p>팀 소속을 검증한 뒤 단일 일정과 반복 일정의 발생 회차를 병합하여 시작 시간순으로 반환한다.</p>
     *
     * @param user 일정을 조회하는 사용자
     * @param teamId 조회할 팀 ID
     * @param year 조회할 연도
     * @param month 조회할 월
     * @return 시작 시간 오름차순으로 정렬된 팀 일정 목록
     * @throws RestApiException 팀을 찾을 수 없거나 사용자가 팀 멤버가 아닌 경우,
     *                          또는 월이 유효하지 않은 경우
     */
    public List<EventListResponse> getTeamEventList(User user, Long teamId, Integer year, Integer month) {
        Team team = getTeam(teamId);
        validateTeamMember(team, user);
        EventOccurrenceService.DateRange dateRange = eventOccurrenceService.createMonthlyDateRange(year, month);

        List<Event> singleEvents = eventRepository.findByTeamAndIsSingleAndStartAtBeforeAndEndAtAfter(
                team, true, dateRange.endAt(), dateRange.startAt());
        List<Event> recurringEvents =
                eventRepository.findByTeamAndIsSingleAndStartAtBefore(team, false, dateRange.endAt());
        List<EventListResponse> recurringOccurrences =
                eventOccurrenceService.expandRecurringEvents(recurringEvents, dateRange, user);

        return eventOccurrenceService.mergeAndSort(singleEvents, recurringOccurrences, user);
    }

    /**
     * 팀 일정을 생성한다.
     *
     * <p>팀 리더 또는 관리자를 주최자로 등록하고 지정된 참여자에게 알림을 생성한다.</p>
     *
     * @param user 일정을 생성하는 사용자
     * @param teamId 일정을 생성할 팀 ID
     * @param request 팀 일정 생성 요청
     * @return 생성된 팀 일정 상세 정보
     * @throws RestApiException 팀을 찾을 수 없거나 일정 관리 권한이 없는 경우,
     *                          또는 참여자나 반복 규칙이 유효하지 않은 경우
     */
    @Transactional
    public EventDetailResponse createTeamEvent(User user, Long teamId, TeamEventCreateRequest request) {
        Team team = getTeam(teamId);
        TeamMember host = validateTeamEventManager(team, user);
        Event event = eventRepository.save(Event.create(team, request));
        RecurrenceRule recurrenceRule = eventRecurrenceService.createRecurrenceRule(event, request);

        List<Participant> participants = createParticipants(event, team, host, request.getParticipants()).stream()
                .map(Participant::create)
                .toList();

        List<User> receivers = eventParticipantRepository.findUsersByEventExcluding(event, user.getUserId());

        notificationService.createNotifications(
                receivers,
                "[" + team.getName() + "] 팀에 새 일정이 추가됐어요",
                "'" + event.getTitle() + "' 일정을 확인해보세요",
                NotificationType.TEAM_SCHEDULE,
                "/team/" + team.getTeamId());

        return EventDetailResponse.create(event, recurrenceRule, team.getName(), true, participants);
    }

    /**
     * 팀 일정을 수정한다.
     *
     * <p>반복 일정의 편집 범위에 맞춰 참여자를 동기화하고 변경 알림을 생성한다.</p>
     *
     * @param user 일정을 수정하는 사용자
     * @param teamId 일정이 속한 팀 ID
     * @param eventId 수정할 일정 ID
     * @param request 팀 일정 수정 요청
     * @return 수정된 팀 일정 상세 정보
     * @throws RestApiException 팀이나 일정을 찾을 수 없거나 일정 관리 권한이 없는 경우,
     *                          또는 참여자, 반복 규칙이나 회차 정보가 유효하지 않은 경우
     */
    @Transactional
    public EventDetailResponse updateTeamEvent(User user, Long teamId, Long eventId, TeamEventUpdateRequest request) {
        Event event = getTeamEvent(teamId, eventId);
        Team team = getTeam(teamId);
        validateTeamEventManager(team, user);

        EventRecurrenceService.EventUpdateResult updateResult =
                eventRecurrenceService.updateEvent(event, team, request);
        boolean isThisInstance = updateResult.editScope() == RecurrenceEditScope.THIS_INSTANCE;
        boolean isThisAndFollowing = updateResult.editScope() == RecurrenceEditScope.THIS_AND_FOLLOWING;

        TeamMember followingEventHost = isThisAndFollowing ? findEventHost(event) : null;

        List<User> receivers;
        List<Participant> participants;

        if (isThisInstance) {
            RecurrenceException recurrenceException = updateResult.recurrenceException();
            participants = syncExceptionParticipants(recurrenceException, team, request.getParticipants());
            receivers = recurrenceExceptionParticipantRepository.findUsersByExceptionExcluding(
                    recurrenceException, user.getUserId());
        } else if (isThisAndFollowing) {
            Event followingEvent = updateResult.event();
            participants =
                    createParticipants(followingEvent, team, followingEventHost, request.getParticipants()).stream()
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
                "/team/" + team.getTeamId());

        boolean isParticipant = Participant.isParticipant(participants, user);
        if (isThisInstance) {
            return EventDetailResponse.createModifiedOccurrence(
                    updateResult.event(),
                    updateResult.recurrenceRule(),
                    updateResult.recurrenceException(),
                    team.getName(),
                    isParticipant,
                    participants);
        }

        return EventDetailResponse.create(
                updateResult.event(), updateResult.recurrenceRule(), team.getName(), isParticipant, participants);
    }

    /**
     * 팀 일정을 삭제한다.
     *
     * <p>반복 일정의 편집 범위에 따라 삭제 대상을 결정하고 참여자에게 알림을 생성한다.</p>
     *
     * @param user 일정을 삭제하는 사용자
     * @param teamId 일정이 속한 팀 ID
     * @param eventId 삭제할 일정 ID
     * @param recurrenceEditScope 반복 일정 편집 범위
     * @param occurrenceAt 편집 대상 회차의 원래 시작 시각
     * @throws RestApiException 팀이나 일정을 찾을 수 없거나 일정 관리 권한이 없는 경우,
     *                          또는 반복 일정이나 회차 정보가 유효하지 않은 경우
     */
    @Transactional
    public void deleteTeamEvent(
            User user, Long teamId, Long eventId, RecurrenceEditScope recurrenceEditScope, LocalDateTime occurrenceAt) {
        Event event = getTeamEvent(teamId, eventId);
        Team team = getTeam(teamId);
        validateTeamEventManager(team, user);

        List<User> receivers;
        if (recurrenceEditScope == RecurrenceEditScope.THIS_INSTANCE) {
            receivers = recurrenceExceptionRepository
                    .findByEventAndOriginalOccurrenceAt(event, occurrenceAt)
                    .map(re -> recurrenceExceptionParticipantRepository.findUsersByExceptionExcluding(
                            re, user.getUserId()))
                    .orElseGet(() -> eventParticipantRepository.findUsersByEventExcluding(event, user.getUserId()));
        } else {
            receivers = eventParticipantRepository.findUsersByEventExcluding(event, user.getUserId());
        }

        notificationService.createNotifications(
                receivers,
                "'" + event.getTitle() + "' 일정이 삭제됐어요",
                user.getName() + "님이 일정을 삭제했어요",
                NotificationType.TEAM_SCHEDULE,
                "/team/" + team.getTeamId());

        if (eventRecurrenceService.deleteEvent(event, recurrenceEditScope, occurrenceAt)) {
            eventParticipantRepository.deleteByEvent(event);
            eventRepository.delete(event);
        }
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 팀 일정의 주최자와 참여자를 생성한다.
     *
     * <p>주최자는 {@link EventRole#HOST}로 항상 포함하고, 중복과 {@code null}을 제거한 유효한 팀 멤버만
     * {@link EventRole#PARTICIPANT}로 저장한다.</p>
     *
     * @param event 참여자를 등록할 일정
     * @param team 참여자가 속한 팀
     * @param host 일정 주최자
     * @param participantIds 참여자로 지정할 팀 멤버 ID 목록
     * @return 저장된 일정 참여자 목록
     * @throws RestApiException 요청된 팀 멤버가 유효한 참여자가 아닌 경우
     */
    private List<EventParticipant> createParticipants(
            Event event, Team team, TeamMember host, List<Long> participantIds) {
        List<EventParticipant> participants = new ArrayList<>();
        participants.add(EventParticipant.create(event, host, EventRole.HOST));

        if (participantIds != null && !participantIds.isEmpty()) {
            List<Long> validIds = participantIds.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .filter(id -> !id.equals(host.getTeamMemberId()))
                    .toList();

            if (!validIds.isEmpty()) {
                Map<Long, TeamMember> memberMap = teamMemberRepository.findByTeamAndIds(team, validIds).stream()
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

    /**
     * 반복 예외 회차의 주최자와 참여자를 생성한다.
     *
     * <p>원본 일정의 주최자를 유지하고, 중복과 {@code null}을 제거한 유효한 팀 멤버만 참여자로 저장한다.</p>
     *
     * @param recurrenceException 참여자를 등록할 반복 예외
     * @param team 참여자가 속한 팀
     * @param host 일정 주최자
     * @param participantIds 참여자로 지정할 팀 멤버 ID 목록
     * @return 저장된 반복 예외 참여자 목록
     * @throws RestApiException 요청된 팀 멤버가 유효한 참여자가 아닌 경우
     */
    private List<RecurrenceExceptionParticipant> createExceptionParticipants(
            RecurrenceException recurrenceException, Team team, TeamMember host, List<Long> participantIds) {
        List<RecurrenceExceptionParticipant> participants = new ArrayList<>();
        participants.add(RecurrenceExceptionParticipant.create(recurrenceException, host, EventRole.HOST));

        if (participantIds != null && !participantIds.isEmpty()) {
            List<Long> validIds = participantIds.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .filter(id -> !id.equals(host.getTeamMemberId()))
                    .toList();

            if (!validIds.isEmpty()) {
                Map<Long, TeamMember> memberMap = teamMemberRepository.findByTeamAndIds(team, validIds).stream()
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

    /**
     * 팀 일정의 참여자를 요청 목록으로 교체한다.
     *
     * <p>기존 참여자를 모두 삭제한 뒤 주최자는 유지하고 요청된 참여자 목록을 다시 저장한다.</p>
     *
     * @param event 참여자를 동기화할 일정
     * @param team 참여자가 속한 팀
     * @param participantIds 참여자로 지정할 팀 멤버 ID 목록
     * @return 동기화된 참여자 정보 목록
     * @throws RestApiException 일정이나 주최자를 찾을 수 없거나 참여자가 유효하지 않은 경우
     */
    private List<Participant> syncParticipants(Event event, Team team, List<Long> participantIds) {
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

    /**
     * 반복 예외 회차의 참여자를 요청 목록으로 교체한다.
     *
     * <p>원본 일정의 주최자를 사용하며 기존 예외 참여자를 모두 삭제한 뒤 요청 목록을 다시 저장한다.</p>
     *
     * @param recurrenceException 참여자를 동기화할 반복 예외
     * @param team 참여자가 속한 팀
     * @param participantIds 참여자로 지정할 팀 멤버 ID 목록
     * @return 동기화된 참여자 정보 목록
     * @throws RestApiException 주최자를 찾을 수 없거나 참여자가 유효하지 않은 경우
     */
    private List<Participant> syncExceptionParticipants(
            RecurrenceException recurrenceException, Team team, List<Long> participantIds) {
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

    /**
     * 팀에 속한 일정을 조회한다.
     *
     * <p>일정 조회 후 요청된 팀 ID와 일정의 팀 ID가 일치하는지 함께 검증한다.</p>
     *
     * @param teamId 일정이 속해야 하는 팀 ID
     * @param eventId 조회할 일정 ID
     * @return 조회된 팀 일정
     * @throws RestApiException 일정을 찾을 수 없거나 일정이 요청된 팀에 속하지 않은 경우
     */
    private Event getTeamEvent(Long teamId, Long eventId) {
        Event event = eventRepository
                .findById(eventId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.EVENT_NOT_FOUND));

        if (!teamId.equals(event.getTeamId())) {
            throw new RestApiException(CustomErrorCode.EVENT_TEAM_MISMATCH);
        }

        return event;
    }

    /**
     * 팀을 조회한다.
     *
     * @param teamId 조회할 팀 ID
     * @return 조회된 팀
     * @throws RestApiException 팀을 찾을 수 없는 경우
     */
    private Team getTeam(Long teamId) {
        return teamRepository.findById(teamId).orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));
    }

    /**
     * 일정의 주최자를 조회한다.
     *
     * @param event 주최자를 조회할 일정
     * @return 일정 주최자
     * @throws RestApiException 일정에 주최자가 등록되지 않은 경우
     */
    private TeamMember findEventHost(Event event) {
        return eventParticipantRepository.findByEvent(event).stream()
                .filter(participant -> participant.getEventRole() == EventRole.HOST)
                .findFirst()
                .map(EventParticipant::getTeamMember)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.EVENT_PARTICIPANT_HOST_NOT_FOUND));
    }

    /**
     * 사용자의 팀 소속을 검증한다.
     *
     * @param team 소속을 확인할 팀
     * @param user 소속을 확인할 사용자
     * @return 사용자에 대응하는 팀 멤버
     * @throws RestApiException 사용자가 팀 멤버가 아닌 경우
     */
    private TeamMember validateTeamMember(Team team, User user) {
        return teamMemberRepository
                .findByTeamAndUser(team, user)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));
    }

    /**
     * 사용자의 팀 일정 관리 권한을 검증한다.
     *
     * <p>{@link TeamRole#LEADER} 또는 {@link TeamRole#MANAGER} 역할만 팀 일정을 관리할 수 있다.</p>
     *
     * @param team 권한을 확인할 팀
     * @param user 권한을 확인할 사용자
     * @return 일정 관리 권한을 가진 팀 멤버
     * @throws RestApiException 사용자가 팀 멤버가 아니거나 팀 리더 또는 관리자가 아닌 경우
     */
    private TeamMember validateTeamEventManager(Team team, User user) {
        TeamMember teamMember = validateTeamMember(team, user);
        if (teamMember.getTeamRole() == TeamRole.LEADER || teamMember.getTeamRole() == TeamRole.MANAGER) {
            return teamMember;
        }

        throw new RestApiException(CustomErrorCode.EVENT_FORBIDDEN);
    }
}
