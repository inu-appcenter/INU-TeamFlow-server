package com.inuteamflow.server.domain.vote.service;

import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.domain.vote.entity.Vote;
import com.inuteamflow.server.domain.vote.entity.VoteAvailability;
import com.inuteamflow.server.domain.vote.entity.VoteParticipant;
import com.inuteamflow.server.domain.vote.entity.VoteTimeSlot;
import com.inuteamflow.server.domain.vote.repository.VoteAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteAvailabilityService {

    private final VoteAvailabilityRepository voteAvailabilityRepository;

    @Transactional
    public void updateVoteAvailabilities(
            VoteParticipant voteParticipant,
            List<VoteTimeSlot> voteTimeSlots
    ) {
        voteAvailabilityRepository.deleteByVoteParticipant(voteParticipant);
        voteAvailabilityRepository.flush();

        List<VoteAvailability> voteAvailabilities = new ArrayList<>();

        for (VoteTimeSlot voteTimeSlot : voteTimeSlots) {
            voteAvailabilities.add(VoteAvailability.create(voteParticipant, voteTimeSlot));
        }

        voteAvailabilityRepository.saveAll(voteAvailabilities);
    }

    // 참석 가능 여부 기록을 삭제한다.
    @Transactional
    public void deleteByVoteId(Long voteId) {
        voteAvailabilityRepository.deleteByVoteId(voteId);
    }

    public Map<Long, Integer> countParticipantsByTimeSlot(
            Vote vote
    ) {
        if (vote.getVoteId() == null) {
            return Map.of();
        }

        Map<Long, Integer> result = new HashMap<>();
        for (Object[] row : voteAvailabilityRepository.countGroupByTimeSlotId(vote.getVoteId())) {
            result.put((Long) row[0], ((Long) row[1]).intValue());
        }

        return result;
    }

    public List<TeamMember> getAvailableTeamMembers(
            List<VoteTimeSlot> voteTimeSlots
    ) {
        if (voteTimeSlots == null || voteTimeSlots.isEmpty()) {
            return List.of();
        }

        List<VoteAvailability> voteAvailabilities = voteAvailabilityRepository.findByVoteTimeSlotIn(voteTimeSlots);
        List<TeamMember> teamMembers = new ArrayList<>();
        Map<Long, TeamMember> teamMemberById = new HashMap<>();
        Map<Long, Integer> selectedSlotCountByTeamMemberId = new HashMap<>();

        for (VoteAvailability voteAvailability : voteAvailabilities) {
            TeamMember teamMember = voteAvailability.getVoteParticipant().getTeamMember();
            Long teamMemberId = teamMember.getTeamMemberId();

            teamMemberById.put(teamMemberId, teamMember);
            selectedSlotCountByTeamMemberId.put(
                    teamMemberId,
                    selectedSlotCountByTeamMemberId.getOrDefault(teamMemberId, 0) + 1
            );
        }

        for (Long teamMemberId : selectedSlotCountByTeamMemberId.keySet()) {
            if (selectedSlotCountByTeamMemberId.get(teamMemberId) == voteTimeSlots.size()) {
                teamMembers.add(teamMemberById.get(teamMemberId));
            }
        }

        return teamMembers;
    }
}
