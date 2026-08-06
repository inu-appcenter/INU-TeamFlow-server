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
public class VoteParticipantService {

    private final VoteParticipantRepository voteParticipantRepository;
    private final TeamMemberRepository teamMemberRepository;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 투표 대상으로 지정된 팀 멤버를 투표 참여자로 생성한다.
     *
     * <p>중복된 팀 멤버 ID는 하나로 처리하며, {@code null}이거나 비어 있는 목록은
     * 투표 참여자를 생성하지 않는다.</p>
     *
     * @param vote 참여자를 등록할 투표
     * @param teamMemberIds 투표 대상으로 지정할 팀 멤버 ID 목록
     * @return 투표 참여자로 등록된 팀 멤버 목록
     * @throws RestApiException 해당 팀에 속하지 않는 팀 멤버 ID가 포함된 경우
     */
    @Transactional
    public List<TeamMember> createVoteParticipants(Vote vote, List<Long> teamMemberIds) {
        if (teamMemberIds == null || teamMemberIds.isEmpty()) {
            return List.of();
        }

        List<Long> uniqueIds =
                teamMemberIds.stream().filter(Objects::nonNull).distinct().toList();

        if (uniqueIds.isEmpty()) {
            return List.of();
        }

        Map<Long, TeamMember> memberMap = teamMemberRepository.findByTeamAndIds(vote.getTeam(), uniqueIds).stream()
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

    /**
     * 투표와 팀 멤버에 해당하는 투표 참여자를 조회한다.
     *
     * @param vote 참여자를 조회할 투표
     * @param teamMember 조회할 팀 멤버
     * @return 조회된 투표 참여자
     * @throws RestApiException 해당하는 투표 참여자를 찾을 수 없는 경우
     */
    public VoteParticipant getVoteParticipant(Vote vote, TeamMember teamMember) {
        return voteParticipantRepository
                .findByVoteAndTeamMember(vote, teamMember)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.VOTE_PARTICIPANT_NOT_FOUND));
    }

    /**
     * 투표 참여자를 참여 완료 여부에 따라 분류하여 조회한다.
     *
     * @param vote 참여 현황을 조회할 투표
     * @return 참여 완료자와 미완료자로 분류된 투표 참여 현황
     */
    public VoteParticipants getVoteParticipants(Vote vote) {
        List<VoterInfoResponse> completedVoters = new ArrayList<>();
        List<VoterInfoResponse> uncompletedVoters = new ArrayList<>();

        for (VoteParticipant voteParticipant : voteParticipantRepository.findByVote(vote)) {
            if (Boolean.TRUE.equals(voteParticipant.getHasCompleted())) {
                completedVoters.add(VoterInfoResponse.from(voteParticipant));
                continue;
            }

            uncompletedVoters.add(VoterInfoResponse.from(voteParticipant));
        }

        return new VoteParticipants(completedVoters, uncompletedVoters);
    }

    /**
     * 투표 참여자 중 지정한 사용자를 제외한 사용자 목록을 조회한다.
     *
     * @param vote 참여자를 조회할 투표
     * @param excludeUserId 결과에서 제외할 사용자 ID
     * @return 지정한 사용자를 제외한 투표 참여자 목록
     */
    public List<User> getUsersByVoteExcluding(Vote vote, Long excludeUserId) {
        return voteParticipantRepository.findByVote(vote).stream()
                .map(vp -> vp.getTeamMember().getUser())
                .filter(u -> !u.getUserId().equals(excludeUserId))
                .toList();
    }

    /**
     * 투표에 등록된 모든 참여자를 삭제한다.
     *
     * @param voteId 참여자를 삭제할 투표 ID
     */
    @Transactional
    public void deleteByVoteId(Long voteId) {
        voteParticipantRepository.deleteByVoteId(voteId);
    }

    /**
     * 사용자가 투표 대상으로 지정되어 있는지 확인한다.
     *
     * @param vote 투표
     * @param user 확인할 사용자
     * @return 투표 대상자이면 {@code true}, 그렇지 않으면 {@code false}
     */
    public boolean isVoter(Vote vote, User user) {
        return voteParticipantRepository.existsByVoteIdAndUserId(vote.getVoteId(), user.getUserId());
    }

    /**
     * 사용자가 투표 대상자로 지정된 투표 목록을 조회한다.
     *
     * @param user 투표 목록을 조회할 사용자
     * @return 사용자가 투표 대상자로 지정된 투표 목록
     */
    public List<Vote> getVotesByUser(User user) {
        return voteParticipantRepository.findVotesByUser(user);
    }

    public record VoteParticipants(
            List<VoterInfoResponse> completedVoters, List<VoterInfoResponse> uncompletedVoters) {}
}
