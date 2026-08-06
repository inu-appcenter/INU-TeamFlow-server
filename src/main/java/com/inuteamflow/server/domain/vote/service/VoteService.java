package com.inuteamflow.server.domain.vote.service;

import com.inuteamflow.server.domain.event.dto.response.EventDetailResponse;
import com.inuteamflow.server.domain.notification.enums.NotificationType;
import com.inuteamflow.server.domain.notification.service.NotificationService;
import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.domain.team.enums.TeamRole;
import com.inuteamflow.server.domain.team.repository.TeamMemberRepository;
import com.inuteamflow.server.domain.team.repository.TeamRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.vote.dto.request.EventVoteCreateRequest;
import com.inuteamflow.server.domain.vote.dto.request.EventVoteTimeSelectRequest;
import com.inuteamflow.server.domain.vote.dto.request.EventVoteTimeSlotSelectRequest;
import com.inuteamflow.server.domain.vote.dto.response.EventVoteResponse;
import com.inuteamflow.server.domain.vote.dto.response.EventVoteTimeSlotResponse;
import com.inuteamflow.server.domain.vote.entity.Vote;
import com.inuteamflow.server.domain.vote.entity.VoteDate;
import com.inuteamflow.server.domain.vote.entity.VoteParticipant;
import com.inuteamflow.server.domain.vote.entity.VoteTimeSlot;
import com.inuteamflow.server.domain.vote.repository.*;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteService {

    private final VoteParticipantService voteParticipantService;
    private final VoteTimeService voteTimeService;
    private final VoteAvailabilityService voteAvailabilityService;
    private final VoteResultService voteResultService;
    private final NotificationService notificationService;

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final VoteRepository voteRepository;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 팀에 생성된 모든 투표를 조회한다.
     *
     * <p>각 투표에는 요청자의 투표 대상자 여부와 투표 생성자 여부가 함께 포함된다.</p>
     *
     * @param user 투표 목록을 조회하는 사용자
     * @param teamId 조회할 팀 ID
     * @return 팀에 생성된 투표 목록
     * @throws RestApiException 팀을 찾을 수 없는 경우
     */
    public List<EventVoteResponse> getVotes(User user, Long teamId) {
        Team team = getTeamById(teamId);
        List<EventVoteResponse> responses = new ArrayList<>();

        for (Vote vote : voteRepository.findByTeam(team)) {
            boolean isVoter = voteParticipantService.isVoter(vote, user);

            responses.add(
                    createEventVoteResponse(vote, isVoter, vote.getCreatedBy().equals(user.getUserId())));
        }

        return responses;
    }

    /**
     * 팀에 일정 투표를 생성한다.
     *
     * <p>투표 참여자, 투표 날짜와 시간 슬롯을 함께 생성하고,
     * 투표 생성자를 제외한 참여자에게 알림을 전송한다.</p>
     *
     * @param user 투표를 생성하는 사용자
     * @param teamId 투표를 생성할 팀 ID
     * @param request 투표 내용, 참여자 및 후보 시간 정보
     * @return 생성된 투표 정보
     * @throws RestApiException 팀을 찾을 수 없거나 사용자가 해당 팀의 멤버가 아닌 경우,
     *                          또는 참여자나 후보 시간 정보가 유효하지 않은 경우
     */
    @Transactional
    public EventVoteResponse createVote(User user, Long teamId, EventVoteCreateRequest request) {
        Team team = getTeamById(teamId);
        validateTeamMember(team, user);

        Vote vote = Vote.create(team, request);
        voteRepository.save(vote);

        List<TeamMember> voteMembers = voteParticipantService.createVoteParticipants(vote, request.getParticipants());

        List<VoteDate> dates = voteTimeService.createVoteDates(vote, request.getDates());
        voteTimeService.createVoteTimeSlots(dates, request);

        List<User> receivers = voteMembers.stream()
                .map(TeamMember::getUser)
                .filter(u -> !u.getUserId().equals(user.getUserId()))
                .toList();

        notificationService.createNotifications(
                receivers,
                "[" + team.getName() + "] 팀에서 일정 투표가 시작됐어요",
                "'" + vote.getTitle() + "' 투표에 참여해주세요",
                NotificationType.CALENDAR,
                "/team/" + team.getTeamId() + "/vote/" + vote.getVoteId());
        boolean isVoter = voteParticipantService.isVoter(vote, user);

        return createEventVoteResponse(vote, isVoter, vote.getCreatedBy().equals(user.getUserId()));
    }

    /**
     * 투표의 상세 정보를 조회한다.
     *
     * <p>요청자의 투표 대상자 여부와 투표 생성자 여부가 함께 포함된다.</p>
     *
     * @param user 투표를 조회하는 사용자
     * @param voteId 조회할 투표 ID
     * @return 투표 상세 정보
     * @throws RestApiException 투표를 찾을 수 없는 경우
     */
    public EventVoteResponse getVote(User user, Long voteId) {
        Vote vote = getVoteById(voteId);
        boolean isVoter = voteParticipantService.isVoter(vote, user);

        return createEventVoteResponse(vote, isVoter, vote.getCreatedBy().equals(user.getUserId()));
    }

    /**
     * 요청자가 투표 대상자로 지정된 투표 목록을 조회한다.
     *
     * <p>반환되는 모든 투표의 {@code isVoter} 값은 {@code true}이며,
     * 요청자가 투표 생성자인지 여부가 {@code isCreator}에 포함된다.</p>
     *
     * @param user 투표 목록을 조회할 사용자
     * @return 요청자가 투표 대상자로 지정된 투표 목록
     */
    public List<EventVoteResponse> getMyVotes(User user) {
        return voteParticipantService.getVotesByUser(user).stream()
                .map(vote ->
                        createEventVoteResponse(vote, true, vote.getCreatedBy().equals(user.getUserId())))
                .toList();
    }

    /**
     * 투표의 후보 시간 슬롯과 슬롯별 선택 인원 수를 조회한다.
     *
     * @param voteId 조회할 투표 ID
     * @return 후보 시간 슬롯과 각 슬롯을 선택한 인원 수
     * @throws RestApiException 투표를 찾을 수 없는 경우
     */
    public List<EventVoteTimeSlotResponse> getTimeSlot(Long voteId) {
        Vote vote = getVoteById(voteId);
        List<VoteTimeSlot> voteTimeSlots = voteTimeService.getVoteTimeSlots(vote);
        Map<Long, Integer> participantCountByTimeSlot = voteAvailabilityService.countParticipantsByTimeSlot(vote);
        List<EventVoteTimeSlotResponse> responses = new ArrayList<>();

        for (VoteTimeSlot voteTimeSlot : voteTimeSlots) {
            responses.add(EventVoteTimeSlotResponse.of(
                    voteTimeSlot, participantCountByTimeSlot.getOrDefault(voteTimeSlot.getVoteTimeSlotId(), 0)));
        }

        return responses;
    }

    /**
     * 투표 참여자가 참석 가능한 시간 슬롯을 선택한다.
     *
     * <p>기존 선택 내역을 요청한 시간 슬롯으로 갱신하고 투표 참여를 완료 처리한다.</p>
     *
     * @param user 시간 슬롯을 선택하는 사용자
     * @param voteId 참여할 투표 ID
     * @param request 선택할 시간 슬롯 ID 목록
     * @return 선택된 시간 슬롯과 각 슬롯을 선택한 인원 수
     * @throws RestApiException 투표를 찾을 수 없거나 투표가 열려 있지 않은 경우,
     *                          사용자가 팀 또는 투표 참여자가 아닌 경우,
     *                          또는 선택한 시간 슬롯이 유효하지 않은 경우
     */
    @Transactional
    public List<EventVoteTimeSlotResponse> selectTimeSlot(
            User user, Long voteId, EventVoteTimeSlotSelectRequest request) {
        Vote vote = getVoteById(voteId);
        validateVoteIsOpened(vote);

        TeamMember teamMember = validateTeamMember(vote.getTeam(), user);
        VoteParticipant voteParticipant = voteParticipantService.getVoteParticipant(vote, teamMember);
        List<VoteTimeSlot> voteTimeSlots = voteTimeService.getValidVoteTimeSlots(vote, request.getSlotIdList());

        voteAvailabilityService.updateVoteAvailabilities(voteParticipant, voteTimeSlots);
        voteParticipant.complete();

        Map<Long, Integer> countMap = voteAvailabilityService.countParticipantsByTimeSlot(vote);
        List<EventVoteTimeSlotResponse> responses = new ArrayList<>();

        for (VoteTimeSlot voteTimeSlot : voteTimeSlots) {
            responses.add(EventVoteTimeSlotResponse.of(
                    voteTimeSlot, countMap.getOrDefault(voteTimeSlot.getVoteTimeSlotId(), 0)));
        }

        return responses;
    }

    /**
     * 투표 결과를 확정하고 선택된 시간으로 팀 일정을 생성한다.
     *
     * <p>확정된 시간에 참석 가능한 투표 참여자를 일정 참여자로 등록하고,
     * 투표 생성자를 제외한 참여자에게 일정 확정 알림을 전송한다.</p>
     *
     * @param user 투표 결과를 확정하는 사용자
     * @param voteId 확정할 투표 ID
     * @param request 확정할 일정의 시작 및 종료 시간
     * @return 생성된 일정의 상세 정보
     * @throws RestApiException 투표를 찾을 수 없거나 투표가 열려 있지 않은 경우,
     *                          사용자가 투표 생성자 또는 팀 멤버가 아닌 경우,
     *                          이미 결과가 확정되었거나 확정 시간이 유효하지 않은 경우
     */
    @Transactional
    public EventDetailResponse createVoteResult(User user, Long voteId, EventVoteTimeSelectRequest request) {
        Vote vote = getVoteById(voteId);
        validateVoteIsOpened(vote);
        validateVoteCreator(vote, user);
        TeamMember host = validateTeamMember(vote.getTeam(), user);

        List<VoteTimeSlot> selectedVoteTimeSlots = voteTimeService.getContinuousVoteTimeSlots(
                vote, request.getSelectedStartAt(), request.getSelectedEndAt());

        List<TeamMember> availableTeamMembers = voteAvailabilityService.getAvailableTeamMembers(selectedVoteTimeSlots);

        List<User> receivers = voteParticipantService.getUsersByVoteExcluding(vote, user.getUserId());

        notificationService.createNotifications(
                receivers,
                "\"" + vote.getTitle() + "\" 일정이 확정됐어요",
                "확정된 일정을 캘린더에서 확인해보세요",
                NotificationType.CALENDAR,
                "/team/" + vote.getTeam().getTeamId() + "/vote/" + vote.getVoteId());

        return voteResultService.createVoteResult(vote, host, availableTeamMembers, request);
    }

    /**
     * 투표와 투표에 종속된 데이터를 삭제한다.
     *
     * <p>투표 생성자, 팀장 또는 팀 매니저만 삭제할 수 있다.</p>
     *
     * @param user 투표 삭제를 요청한 사용자
     * @param voteId 삭제할 투표 ID
     * @throws RestApiException 투표를 찾을 수 없거나 사용자가 팀 멤버가 아닌 경우,
     *                          또는 투표 삭제 권한이 없는 경우
     */
    @Transactional
    public void deleteVote(User user, Long voteId) {
        Vote vote = getVoteById(voteId);
        TeamMember requester = validateTeamMember(vote.getTeam(), user);
        validateVoteDeletable(vote, requester);

        deleteVoteCascade(voteId);
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * ID에 해당하는 팀을 조회한다.
     *
     * @param teamId 조회할 팀 ID
     * @return 조회된 팀
     * @throws RestApiException 팀을 찾을 수 없는 경우
     */
    private Team getTeamById(Long teamId) {
        return teamRepository.findById(teamId).orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));
    }

    /**
     * 사용자가 팀에 소속되어 있는지 검증한다.
     *
     * @param team 소속 여부를 확인할 팀
     * @param user 소속 여부를 확인할 사용자
     * @return 사용자에 해당하는 팀 멤버
     * @throws RestApiException 사용자가 해당 팀의 멤버가 아닌 경우
     */
    private TeamMember validateTeamMember(Team team, User user) {
        return teamMemberRepository
                .findByTeamAndUser(team, user)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));
    }

    /**
     * ID에 해당하는 투표를 조회한다.
     *
     * @param voteId 조회할 투표 ID
     * @return 조회된 투표
     * @throws RestApiException 투표를 찾을 수 없는 경우
     */
    private Vote getVoteById(Long voteId) {
        return voteRepository.findById(voteId).orElseThrow(() -> new RestApiException(CustomErrorCode.VOTE_NOT_FOUND));
    }

    /**
     * 투표와 참여 현황을 조합하여 투표 응답을 생성한다.
     *
     * @param vote 투표
     * @param isVoter 요청자의 투표 대상자 여부
     * @param isCreator 요청자의 투표 생성자 여부
     * @return 투표 정보, 후보 날짜 및 참여 현황을 포함한 응답
     */
    private EventVoteResponse createEventVoteResponse(Vote vote, boolean isVoter, boolean isCreator) {
        VoteParticipantService.VoteParticipants voteParticipants = voteParticipantService.getVoteParticipants(vote);

        List<LocalDate> dates = voteTimeService.getVoteDates(vote).stream()
                .map(VoteDate::getDate)
                .toList();

        return EventVoteResponse.of(
                vote,
                isVoter,
                isCreator,
                dates,
                voteParticipants.completedVoters(),
                voteParticipants.uncompletedVoters());
    }

    /**
     * 투표가 참여 가능한 상태인지 검증한다.
     *
     * @param vote 상태를 확인할 투표
     * @throws RestApiException 투표가 열려 있지 않은 경우
     */
    private void validateVoteIsOpened(Vote vote) {
        if (!Boolean.TRUE.equals(vote.getIsOpened())) {
            throw new RestApiException(CustomErrorCode.VOTE_NOT_OPENED);
        }
    }

    /**
     * 사용자가 투표 생성자인지 검증한다.
     *
     * @param vote 생성자를 확인할 투표
     * @param user 생성자 여부를 확인할 사용자
     * @throws RestApiException 사용자가 투표 생성자가 아닌 경우
     */
    private void validateVoteCreator(Vote vote, User user) {
        if (!vote.getCreatedBy().equals(user.getUserId())) {
            throw new RestApiException(CustomErrorCode.VOTE_NOT_CREATOR);
        }
    }

    /**
     * 팀 멤버에게 투표 삭제 권한이 있는지 검증한다.
     *
     * <p>투표 생성자, 팀장 또는 팀 매니저에게 삭제 권한이 있다.</p>
     *
     * @param vote 삭제할 투표
     * @param requester 삭제를 요청한 팀 멤버
     * @throws RestApiException 요청자에게 투표 삭제 권한이 없는 경우
     */
    private void validateVoteDeletable(Vote vote, TeamMember requester) {
        boolean isCreator = vote.getCreatedBy().equals(requester.getUser().getUserId());
        boolean isManager = requester.getTeamRole() == TeamRole.LEADER || requester.getTeamRole() == TeamRole.MANAGER;

        if (!isCreator && !isManager) {
            throw new RestApiException(CustomErrorCode.VOTE_DELETE_FORBIDDEN);
        }
    }

    /**
     * 투표와 연관된 모든 데이터를 순차적으로 삭제한다.
     *
     * <p>다른 도메인에서 투표를 정리할 때 사용하는 내부 협력 메서드로,
     * 사용자 요청에서 호출할 때는 호출자가 사전에 권한을 검증해야 한다.
     * 각 객체의 삭제는 전용 하위 서비스에 위임한다. </p>
     *
     * @param voteId 삭제할 투표 ID
     */
    @Transactional
    public void deleteVoteCascade(Long voteId) {
        voteAvailabilityService.deleteByVoteId(voteId);
        voteResultService.deleteByVoteId(voteId);
        voteParticipantService.deleteByVoteId(voteId);
        voteTimeService.deleteByVoteId(voteId);
        voteRepository.deleteByVoteId(voteId);
    }
}
