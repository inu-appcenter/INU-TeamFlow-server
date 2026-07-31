package com.inuteamflow.server.domain.event.service;

import com.inuteamflow.server.domain.event.dto.EventCreateCommand;
import com.inuteamflow.server.domain.event.dto.EventUpdateCommand;
import com.inuteamflow.server.domain.event.entity.*;
import com.inuteamflow.server.domain.event.enums.RecurrenceEditScope;
import com.inuteamflow.server.domain.event.enums.RecurrenceExceptionType;
import com.inuteamflow.server.domain.event.repository.*;
import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventRecurrenceService {

    private final EventOccurrenceService eventOccurrenceService;
    private final EventRepository eventRepository;
    private final RecurrenceRuleRepository recurrenceRuleRepository;
    private final RecurrenceExceptionRepository recurrenceExceptionRepository;
    private final RecurrenceExceptionParticipantRepository recurrenceExceptionParticipantRepository;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 일정 생성 요청의 반복 규칙을 생성한다.
     *
     * <p>반복 설정이 없으면 저장하지 않고 {@code null}을 반환하며, 있으면 일정 시작 시각을 시리즈 기준으로 사용한다.</p>
     *
     * @param event 반복 규칙을 연결할 일정
     * @param command 일정 생성 명령
     * @return 생성된 반복 규칙 또는 반복 설정이 없으면 {@code null}
     */
    @Transactional
    public RecurrenceRule createRecurrenceRule(Event event, EventCreateCommand command) {
        if (command.getRecurrence() == null) {
            return null;
        }

        return recurrenceRuleRepository.save(RecurrenceRule.create(event, command.getRecurrence(), event.getStartAt()));
    }

    /**
     * 일정 수정 요청의 반복 규칙을 생성한다.
     *
     * <p>단일 일정이 반복 일정으로 전환될 때 일정 시작 시각을 시리즈 기준으로 새 규칙을 저장한다.</p>
     *
     * @param event 반복 규칙을 연결할 일정
     * @param command 일정 수정 명령
     * @return 생성된 반복 규칙 또는 반복 설정이 없으면 {@code null}
     */
    @Transactional
    public RecurrenceRule createRecurrenceRule(Event event, EventUpdateCommand command) {
        if (command.getRecurrence() == null) {
            return null;
        }

        return recurrenceRuleRepository.save(RecurrenceRule.create(event, command.getRecurrence(), event.getStartAt()));
    }

    /**
     * 편집 범위에 따라 일정을 수정한다.
     *
     * <p>반복 일정은 {@link RecurrenceEditScope#ALL_SERIES},
     * {@link RecurrenceEditScope#THIS_INSTANCE},
     * {@link RecurrenceEditScope#THIS_AND_FOLLOWING}으로 분기하여 처리한다.</p>
     *
     * @param event 수정할 일정
     * @param team 일정이 속한 팀 또는 개인 일정이면 {@code null}
     * @param command 일정 수정 명령
     * @return 수정된 일정과 반복 정보
     * @throws RestApiException 반복 규칙이나 대상 회차가 누락되었거나 유효하지 않은 경우
     */
    @Transactional
    public EventUpdateResult updateEvent(Event event, Team team, EventUpdateCommand command) {
        event.increaseSequence();

        if (Boolean.TRUE.equals(event.getIsSingle())) {
            return updateSingleEvent(event, command);
        }

        RecurrenceEditScope editScope = command.getRecurrenceEditScope() == null
                ? RecurrenceEditScope.ALL_SERIES
                : command.getRecurrenceEditScope();

        // THIS_INSTANCE는 해당 회차만 수정하므로 반복 규칙 데이터 불필요
        if (editScope != RecurrenceEditScope.THIS_INSTANCE) {
            validateRecurrenceRequired(command);
        }

        if ((editScope == RecurrenceEditScope.THIS_INSTANCE || editScope == RecurrenceEditScope.THIS_AND_FOLLOWING)
                && command.getOccurrenceAt() == null) {
            throw new RestApiException(CustomErrorCode.EVENT_RECURRENCE_OCCURRENCE_REQUIRED);
        }

        return switch (editScope) {
            case ALL_SERIES -> {
                RecurrenceRule recurrenceRule = updateAllSeries(event, command);
                yield new EventUpdateResult(event, recurrenceRule, null, RecurrenceEditScope.ALL_SERIES);
            }
            case THIS_INSTANCE -> {
                RecurrenceRule recurrenceRule = getRecurrenceRule(event);
                RecurrenceException recurrenceException = updateThisInstance(event, command);
                yield new EventUpdateResult(
                        event, recurrenceRule, recurrenceException, RecurrenceEditScope.THIS_INSTANCE);
            }
            case THIS_AND_FOLLOWING -> {
                FollowingSeries followingSeries = updateThisAndFollowing(event, team, command);
                yield new EventUpdateResult(
                        followingSeries.event(),
                        followingSeries.recurrenceRule(),
                        null,
                        RecurrenceEditScope.THIS_AND_FOLLOWING);
            }
        };
    }

    /**
     * 단일 일정을 수정한다.
     *
     * <p>반복 설정이 추가되면 단일 일정을 반복 일정으로 전환한다.</p>
     *
     * @param event 수정할 단일 일정
     * @param command 일정 수정 명령
     * @return 수정된 일정과 반복 정보
     */
    private EventUpdateResult updateSingleEvent(Event event, EventUpdateCommand command) {
        event.update(command);
        if (command.getRecurrence() == null) {
            return new EventUpdateResult(event, null, null, RecurrenceEditScope.ALL_SERIES);
        }

        event.changeToRecurring();
        RecurrenceRule recurrenceRule = createRecurrenceRule(event, command);

        return new EventUpdateResult(event, recurrenceRule, null, RecurrenceEditScope.ALL_SERIES);
    }

    /**
     * 반복 일정 전체를 수정한다.
     *
     * <p>기존 반복 예외와 예외 참여자를 모두 삭제한다.</p>
     *
     * @param event 수정할 반복 일정
     * @param command 일정 수정 명령
     * @return 수정된 반복 규칙
     * @throws RestApiException 반복 규칙을 찾을 수 없는 경우
     */
    private RecurrenceRule updateAllSeries(Event event, EventUpdateCommand command) {
        RecurrenceRule recurrenceRule = getRecurrenceRule(event);

        event.update(command);
        recurrenceRule.update(command.getRecurrence(), event.getStartAt());
        recurrenceExceptionParticipantRepository.deleteByEvent(event);
        recurrenceExceptionRepository.deleteByEvent(event);

        return recurrenceRule;
    }

    /**
     * 반복 일정의 특정 회차를 수정한다.
     *
     * <p>동일 회차의 예외가 있으면 갱신하고, 없으면 {@link RecurrenceExceptionType#MODIFIED} 예외를 생성한다.</p>
     *
     * @param event 수정할 반복 일정
     * @param command 일정 수정 명령
     * @return 생성되거나 수정된 반복 예외
     * @throws RestApiException 반복 규칙이나 대상 회차를 찾을 수 없는 경우
     */
    private RecurrenceException updateThisInstance(Event event, EventUpdateCommand command) {
        RecurrenceRule recurrenceRule = getRecurrenceRule(event);
        validateOccurrence(event, recurrenceRule, command.getOccurrenceAt());

        RecurrenceException recurrenceException = recurrenceExceptionRepository
                .findByEventAndOriginalOccurrenceAt(event, command.getOccurrenceAt())
                .orElseGet(
                        () -> recurrenceExceptionRepository.save(RecurrenceException.createModified(event, command)));
        recurrenceException.update(command);

        return recurrenceException;
    }

    /**
     * 지정 회차부터 새로운 반복 일정으로 분리한다.
     *
     * <p>기존 규칙은 대상 회차 직전에 종료하고 대상 회차 이후의 예외를 제거한 뒤 새 일정과 반복 규칙을 생성한다.</p>
     *
     * @param event 분리할 기존 반복 일정
     * @param team 일정이 속한 팀 또는 개인 일정이면 {@code null}
     * @param command 일정 수정 명령
     * @return 새로 생성된 이후 일정과 반복 규칙
     * @throws RestApiException 반복 규칙이나 대상 회차를 찾을 수 없는 경우
     */
    private FollowingSeries updateThisAndFollowing(Event event, Team team, EventUpdateCommand command) {
        RecurrenceRule recurrenceRule = getRecurrenceRule(event);
        validateOccurrence(event, recurrenceRule, command.getOccurrenceAt());

        recurrenceRule.finishBefore(command.getOccurrenceAt());
        recurrenceExceptionParticipantRepository.deleteByEventAndOriginalOccurrenceAtGreaterThanEqual(
                event, command.getOccurrenceAt());
        recurrenceExceptionRepository.deleteByEventAndOriginalOccurrenceAtGreaterThanEqual(
                event, command.getOccurrenceAt());

        Event followingEvent = team == null
                ? eventRepository.save(Event.createRecurring(command))
                : eventRepository.save(Event.createRecurring(team, command));

        RecurrenceRule followingRule = recurrenceRuleRepository.save(
                RecurrenceRule.create(followingEvent, command.getRecurrence(), followingEvent.getStartAt()));

        return new FollowingSeries(followingEvent, followingRule);
    }

    /**
     * 편집 범위에 따라 일정을 삭제한다.
     *
     * <p>반복 일정은 {@link RecurrenceEditScope#ALL_SERIES},
     * {@link RecurrenceEditScope#THIS_INSTANCE},
     * {@link RecurrenceEditScope#THIS_AND_FOLLOWING}으로 분기하여 처리한다.</p>
     *
     * @param event 삭제할 일정
     * @param recurrenceEditScope 반복 일정 편집 범위
     * @param occurrenceAt 편집 대상 회차의 원래 시작 시각
     * @return 일정 엔티티를 삭제해야 하면 {@code true}, 그렇지 않으면 {@code false}
     * @throws RestApiException 대상 회차가 누락되었거나 반복 규칙 또는 회차를 찾을 수 없는 경우
     */
    @Transactional
    public boolean deleteEvent(Event event, RecurrenceEditScope recurrenceEditScope, LocalDateTime occurrenceAt) {
        if (Boolean.TRUE.equals(event.getIsSingle())) {
            return true;
        }

        RecurrenceEditScope editScope =
                recurrenceEditScope == null ? RecurrenceEditScope.ALL_SERIES : recurrenceEditScope;

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

    /**
     * 반복 일정 전체의 반복 데이터를 삭제한다.
     *
     * <p>반복 예외 참여자, 반복 예외, 반복 규칙 순으로 연관 데이터를 삭제한다.</p>
     *
     * @param event 반복 데이터를 삭제할 일정
     */
    private void deleteAllSeries(Event event) {
        recurrenceExceptionParticipantRepository.deleteByEvent(event);
        recurrenceExceptionRepository.deleteByEvent(event);
        recurrenceRuleRepository.deleteByEvent(event);
    }

    /**
     * 반복 일정의 특정 회차를 취소한다.
     *
     * <p>기존 수정 예외의 참여자를 제거하고 해당 회차 예외를 {@link RecurrenceExceptionType#CANCELLED} 상태로 변경한다.</p>
     *
     * @param event 회차를 취소할 반복 일정
     * @param occurrenceAt 취소할 회차의 원래 시작 시각
     * @throws RestApiException 반복 규칙이나 대상 회차를 찾을 수 없는 경우
     */
    private void deleteThisInstance(Event event, LocalDateTime occurrenceAt) {
        RecurrenceRule recurrenceRule = getRecurrenceRule(event);
        validateOccurrence(event, recurrenceRule, occurrenceAt);

        recurrenceExceptionParticipantRepository.deleteByEventAndOccurrenceAt(event, occurrenceAt);
        RecurrenceException recurrenceException = recurrenceExceptionRepository
                .findByEventAndOriginalOccurrenceAt(event, occurrenceAt)
                .orElseGet(() ->
                        recurrenceExceptionRepository.save(RecurrenceException.createCancelled(event, occurrenceAt)));
        recurrenceException.cancel();
    }

    /**
     * 반복 일정의 지정 회차와 이후 회차를 종료한다.
     *
     * <p>반복 규칙을 대상 회차 직전에 종료하고 대상 회차 이후의 예외와 예외 참여자를 삭제한다.</p>
     *
     * @param event 종료할 반복 일정
     * @param occurrenceAt 종료를 시작할 회차의 원래 시작 시각
     * @throws RestApiException 반복 규칙이나 대상 회차를 찾을 수 없는 경우
     */
    private void deleteThisAndFollowing(Event event, LocalDateTime occurrenceAt) {
        RecurrenceRule recurrenceRule = getRecurrenceRule(event);
        validateOccurrence(event, recurrenceRule, occurrenceAt);

        recurrenceRule.finishBefore(occurrenceAt);
        recurrenceExceptionParticipantRepository.deleteByEventAndOriginalOccurrenceAtGreaterThanEqual(
                event, occurrenceAt);
        recurrenceExceptionRepository.deleteByEventAndOriginalOccurrenceAtGreaterThanEqual(event, occurrenceAt);
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 일정의 반복 규칙을 조회한다.
     *
     * @param event 반복 규칙을 조회할 일정
     * @return 일정의 반복 규칙
     * @throws RestApiException 반복 규칙을 찾을 수 없는 경우
     */
    private RecurrenceRule getRecurrenceRule(Event event) {
        return recurrenceRuleRepository
                .findByEvent(event)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.EVENT_RECURRENCE_RULE_NOT_FOUND));
    }

    /**
     * 일정 수정 명령에 반복 설정이 포함되었는지 검증한다.
     *
     * @param command 검증할 일정 수정 명령
     * @throws RestApiException 반복 설정이 없는 경우
     */
    private void validateRecurrenceRequired(EventUpdateCommand command) {
        if (command.getRecurrence() == null) {
            throw new RestApiException(CustomErrorCode.EVENT_RECURRENCE_REQUIRED);
        }
    }

    /**
     * 지정 시각이 실제 반복 회차인지 검증한다.
     *
     * @param event 검증할 반복 일정
     * @param recurrenceRule 일정의 반복 규칙
     * @param occurrenceAt 검증할 회차 시작 시각
     * @throws RestApiException 지정 시각에 반복 회차가 없는 경우
     */
    private void validateOccurrence(Event event, RecurrenceRule recurrenceRule, LocalDateTime occurrenceAt) {
        if (!eventOccurrenceService.existsOccurrence(event, recurrenceRule, occurrenceAt)) {
            throw new RestApiException(CustomErrorCode.EVENT_RECURRENCE_OCCURRENCE_NOT_FOUND);
        }
    }

    private record FollowingSeries(Event event, RecurrenceRule recurrenceRule) {}

    public record EventUpdateResult(
            Event event,
            RecurrenceRule recurrenceRule,
            RecurrenceException recurrenceException,
            RecurrenceEditScope editScope) {}
}
