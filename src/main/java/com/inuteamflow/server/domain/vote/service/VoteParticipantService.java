package com.inuteamflow.server.domain.vote.service;

import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.domain.team.repository.TeamMemberRepository;
import com.inuteamflow.server.domain.vote.entity.Vote;
import com.inuteamflow.server.domain.vote.entity.VoteParticipant;
import com.inuteamflow.server.domain.vote.repository.VoteParticipantRepository;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteParticipantService {

    private final VoteParticipantRepository voteParticipantRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Transactional
    public void createVoteParticipants(
            Vote vote,
            List<Long> teamMemberIds
    ) {
        if (teamMemberIds == null || teamMemberIds.isEmpty()) {
            return;
        }

        List<Long> uniqueIds = teamMemberIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (uniqueIds.isEmpty()) {
            return;
        }

        Map<Long, TeamMember> memberMap = teamMemberRepository.findByTeamAndIds(vote.getTeam(), uniqueIds)
                .stream()
                .collect(Collectors.toMap(TeamMember::getTeamMemberId, Function.identity()));

        List<VoteParticipant> participants = new ArrayList<>();
        for (Long id : uniqueIds) {
            if (!memberMap.containsKey(id)) {
                throw new RestApiException(CustomErrorCode.VOTE_PARTICIPANT_INVALID);
            }
            participants.add(VoteParticipant.create(vote, memberMap.get(id)));
        }

        voteParticipantRepository.saveAll(participants);
    }

    public VoteParticipant getVoteParticipant(
            Vote vote,
            TeamMember teamMember
    ) {
        return voteParticipantRepository.findByVoteAndTeamMember(vote, teamMember)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.VOTE_PARTICIPANT_NOT_FOUND));
    }

    public VoteParticipantNames getVoteParticipantNames(
            Vote vote
    ) {
        List<String> completedVoterNames = new ArrayList<>();
        List<String> uncompletedVoterNames = new ArrayList<>();

        for (VoteParticipant voteParticipant : voteParticipantRepository.findByVote(vote)) {
            String voterName = voteParticipant.getTeamMember().getUser().getName();

            if (Boolean.TRUE.equals(voteParticipant.getHasCompleted())) {
                completedVoterNames.add(voterName);
                continue;
            }

            uncompletedVoterNames.add(voterName);
        }

        return new VoteParticipantNames(completedVoterNames, uncompletedVoterNames);
    }

    public record VoteParticipantNames(
            List<String> completedVoterNames,
            List<String> uncompletedVoterNames
    ) {
    }
}
