package com.inuteamflow.server.domain.vote.service;

import com.inuteamflow.server.domain.event.dto.Participant;
import com.inuteamflow.server.domain.event.dto.response.EventDetailResponse;
import com.inuteamflow.server.domain.event.entity.Event;
import com.inuteamflow.server.domain.event.entity.EventParticipant;
import com.inuteamflow.server.domain.event.enums.EventRole;
import com.inuteamflow.server.domain.event.repository.EventParticipantRepository;
import com.inuteamflow.server.domain.event.repository.EventRepository;
import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.domain.vote.dto.VoteResultEventCreateCommand;
import com.inuteamflow.server.domain.vote.dto.request.EventVoteTimeSelectRequest;
import com.inuteamflow.server.domain.vote.entity.Vote;
import com.inuteamflow.server.domain.vote.entity.VoteResult;
import com.inuteamflow.server.domain.vote.repository.VoteResultRepository;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteResultService {

    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final VoteResultRepository voteResultRepository;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 투표 결과를 확정하여 팀 일정과 투표 결과를 생성한다.
     *
     * <p>투표 생성자를 일정 주최자로 등록하고 참석 가능한 팀 멤버를 일정 참여자로
     * 등록한 후 투표를 종료한다.</p>
     *
     * @param vote 결과를 확정할 투표
     * @param host 생성할 일정의 주최자
     * @param participants 생성할 일정의 참여자
     * @param request 확정된 일정의 종일 여부와 시작·종료 시간
     * @return 생성된 일정의 상세 정보
     * @throws RestApiException 투표 결과가 이미 존재하거나 확정 시간이 유효하지 않은 경우
     */
    @Transactional
    public EventDetailResponse createVoteResult(
            Vote vote, TeamMember host, List<TeamMember> participants, EventVoteTimeSelectRequest request) {
        validateVoteResultCreatable(vote, request);

        Event event =
                eventRepository.save(Event.create(vote.getTeam(), new VoteResultEventCreateCommand(vote, request)));
        List<Participant> eventParticipants = createEventParticipants(event, host, participants).stream()
                .map(Participant::from)
                .toList();
        voteResultRepository.save(VoteResult.create(
                vote, event, request.getIsAllDay(), request.getSelectedStartAt(), request.getSelectedEndAt()));
        vote.close();

        return EventDetailResponse.of(
                event,
                null,
                vote.getTeam().getName(),
                Participant.isParticipant(eventParticipants, host.getUser()),
                eventParticipants);
    }

    /**
     * 투표에 연결된 투표 결과를 삭제한다.
     *
     * @param voteId 결과를 삭제할 투표 ID
     */
    @Transactional
    public void deleteByVoteId(Long voteId) {
        voteResultRepository.deleteByVoteId(voteId);
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 투표 결과를 생성할 수 있는 상태인지 검증한다.
     *
     * @param vote 결과를 생성할 투표
     * @param request 확정할 일정 시간 정보
     * @throws RestApiException 투표 결과가 이미 존재하거나 확정 시간이 유효하지 않은 경우
     */
    private void validateVoteResultCreatable(Vote vote, EventVoteTimeSelectRequest request) {
        if (voteResultRepository.existsByVote(vote)) {
            throw new RestApiException(CustomErrorCode.VOTE_RESULT_ALREADY_EXISTS);
        }

        if (request.getSelectedStartAt() == null || request.getSelectedEndAt() == null) {
            throw new RestApiException(CustomErrorCode.VOTE_RESULT_TIME_INVALID);
        }

        if (!request.getSelectedStartAt().isBefore(request.getSelectedEndAt())) {
            throw new RestApiException(CustomErrorCode.VOTE_RESULT_TIME_INVALID);
        }
    }

    /**
     * 일정 주최자와 참석 가능한 팀 멤버를 일정 참여자로 생성한다.
     *
     * <p>주최자는 {@link EventRole#HOST}로 등록하고, 나머지 팀 멤버는
     * {@link EventRole#PARTICIPANT}로 등록한다. 참여자 목록에 포함된 주최자는
     * 중복 등록하지 않는다.</p>
     *
     * @param event 참여자를 등록할 일정
     * @param host 일정 주최자
     * @param participants 일정에 참여할 팀 멤버 목록
     * @return 저장된 일정 참여자 목록
     */
    private List<EventParticipant> createEventParticipants(
            Event event, TeamMember host, List<TeamMember> participants) {
        List<EventParticipant> eventParticipants = new ArrayList<>();
        eventParticipants.add(EventParticipant.create(event, host, EventRole.HOST));

        for (TeamMember participant : participants) {
            if (participant.getTeamMemberId().equals(host.getTeamMemberId())) {
                continue;
            }

            eventParticipants.add(EventParticipant.create(event, participant, EventRole.PARTICIPANT));
        }

        return eventParticipantRepository.saveAll(eventParticipants);
    }
}
