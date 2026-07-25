package com.inuteamflow.server.domain.vote.service;

import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.domain.team.repository.TeamMemberRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.vote.dto.response.VoterInfoResponse;
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
    public List<TeamMember> createVoteParticipants(
            Vote vote,
            List<Long> teamMemberIds
    ) {
        if (teamMemberIds == null || teamMemberIds.isEmpty()) {
            return List.of();
        }

        List<Long> uniqueIds = teamMemberIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (uniqueIds.isEmpty()) {
            return List.of();
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

        return uniqueIds.stream().map(memberMap::get).toList();
    }

    public VoteParticipant getVoteParticipant(
            Vote vote,
            TeamMember teamMember
    ) {
        return voteParticipantRepository.findByVoteAndTeamMember(vote, teamMember)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.VOTE_PARTICIPANT_NOT_FOUND));
    }

    public VoteParticipants getVoteParticipants(
            Vote vote
    ) {
        List<VoterInfoResponse> completedVoters = new ArrayList<>();
        List<VoterInfoResponse> uncompletedVoters = new ArrayList<>();

        for (VoteParticipant voteParticipant : voteParticipantRepository.findByVote(vote)) {
            if (Boolean.TRUE.equals(voteParticipant.getHasCompleted())) {
                completedVoters.add(VoterInfoResponse.create(voteParticipant));
                continue;
            }

            uncompletedVoters.add(VoterInfoResponse.create(voteParticipant));
        }

        return new VoteParticipants(completedVoters, uncompletedVoters);
    }

    public List<User> getUsersByVoteExcluding(Vote vote, Long excludeUserId) {
        return voteParticipantRepository.findByVote(vote).stream()
                .map(vp -> vp.getTeamMember().getUser())
                .filter(u -> !u.getUserId().equals(excludeUserId))
                .toList();
    }

    public record VoteParticipants(
            List<VoterInfoResponse> completedVoters,
            List<VoterInfoResponse> uncompletedVoters
    ) {
    }

    // 투표 참여자를 삭제한다.
    @Transactional
    public void deleteByVoteId(Long voteId) {
        voteParticipantRepository.deleteByVoteId(voteId);
    }
}
