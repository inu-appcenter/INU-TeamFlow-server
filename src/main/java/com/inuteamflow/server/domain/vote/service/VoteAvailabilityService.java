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

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 투표 참여자의 참석 가능한 시간 슬롯을 갱신한다.
     *
     * <p>기존 참석 가능 기록을 모두 삭제한 후 요청된 시간 슬롯으로 다시 저장한다.</p>
     *
     * @param voteParticipant 참석 가능 시간을 갱신할 투표 참여자
     * @param voteTimeSlots 참여자가 참석 가능한 시간 슬롯 목록
     */
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

    /**
     * 투표에 등록된 모든 참석 가능 기록을 삭제한다.
     *
     * @param voteId 참석 가능 기록을 삭제할 투표 ID
     */
    @Transactional
    public void deleteByVoteId(Long voteId) {
        voteAvailabilityRepository.deleteByVoteId(voteId);
    }

    /**
     * 투표의 시간 슬롯별 선택 인원 수를 집계한다.
     *
     * @param vote 선택 인원을 집계할 투표
     * @return 시간 슬롯 ID를 키로 하고 선택 인원 수를 값으로 하는 맵
     */
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

    /**
     * 선택된 모든 시간 슬롯에 참석 가능한 팀 멤버를 조회한다.
     *
     * <p>하나 이상의 선택된 시간 슬롯에 참석 가능한 멤버가 아니라,
     * 전달된 모든 시간 슬롯을 선택한 멤버만 반환한다.</p>
     *
     * @param voteTimeSlots 참석 가능 여부를 확인할 시간 슬롯 목록
     * @return 모든 시간 슬롯에 참석 가능한 팀 멤버 목록
     */
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
