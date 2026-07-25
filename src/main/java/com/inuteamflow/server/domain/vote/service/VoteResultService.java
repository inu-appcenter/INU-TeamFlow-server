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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteResultService {

    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final VoteResultRepository voteResultRepository;

    // 투표 결과를 확정하고 일정과 투표 결과를 생성한다.
    @Transactional
    public EventDetailResponse createVoteResult(
            Vote vote,
            TeamMember host,
            List<TeamMember> participants,
            EventVoteTimeSelectRequest request
    ) {
        validateVoteResultCreatable(vote, request);

        Event event = eventRepository.save(Event.create(
                vote.getTeam(),
                new VoteResultEventCreateCommand(vote, request)
        ));
        List<Participant> eventParticipants = createEventParticipants(event, host, participants).stream()
                .map(Participant::create)
                .toList();
        voteResultRepository.save(VoteResult.create(
                vote,
                event,
                request.getIsAllDay(),
                request.getSelectedStartAt(),
                request.getSelectedEndAt()
        ));
        vote.close();

        return EventDetailResponse.create(
                event,
                null,
                vote.getTeam().getName(),
                Participant.isParticipant(eventParticipants, host.getUser()),
                eventParticipants
        );
    }

    // 투표 결과를 삭제한다.
    @Transactional
    public void deleteByVoteId(Long voteId) {
        voteResultRepository.deleteByVoteId(voteId);
    }

    // 투표 결과 생성 가능 여부를 검증한다.
    private void validateVoteResultCreatable(
            Vote vote,
            EventVoteTimeSelectRequest request
    ) {
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

    // 최종 선택 시간대에 공통적으로 참석 가능한 투표자를 일정 참여자로 편입한다.
    private List<EventParticipant> createEventParticipants(
            Event event,
            TeamMember host,
            List<TeamMember> participants
    ) {
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
