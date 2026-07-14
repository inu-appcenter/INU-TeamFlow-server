package com.inuteamflow.server.domain.vote.service;

import com.inuteamflow.server.domain.event.dto.response.EventDetailResponse;
import com.inuteamflow.server.domain.notification.enums.NotificationType;
import com.inuteamflow.server.domain.notification.service.NotificationService;
import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.team.entity.TeamMember;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    // 팀의 투표 목록을 조회한다.
    public List<EventVoteResponse> getVoteList(
            Long teamId
    ) {
        Team team = getTeamById(teamId);
        List<EventVoteResponse> responses = new ArrayList<>();

        for (Vote vote : voteRepository.findByTeam(team)) {
            responses.add(createEventVoteResponse(vote));
        }

        return responses;
    }

    // 투표를 생성하고 참여자와 시간 슬롯을 함께 생성한다.
    @Transactional
    public EventVoteResponse createVote(
            User user,
            Long teamId,
            EventVoteCreateRequest request
    ) {
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
                NotificationType.TEAM_SCHEDULE,
                "/team/" + team.getTeamId() + "/vote/" + vote.getVoteId()
        );

        return createEventVoteResponse(vote);
    }

    // 투표 상세 정보를 조회한다.
    public EventVoteResponse getVote(
            Long voteId
    ) {
        Vote vote = getVoteById(voteId);
        return createEventVoteResponse(vote);
    }

    // 투표의 시간 슬롯 목록과 선택 인원 수를 조회한다.
    public List<EventVoteTimeSlotResponse> getTimeSlot(
            Long voteId
    ) {
        Vote vote = getVoteById(voteId);
        List<VoteTimeSlot> voteTimeSlots = voteTimeService.getVoteTimeSlots(vote);
        Map<Long, Integer> participantCountByTimeSlot = voteAvailabilityService.countParticipantsByTimeSlot(vote);
        List<EventVoteTimeSlotResponse> responses = new ArrayList<>();

        for (VoteTimeSlot voteTimeSlot : voteTimeSlots) {
            responses.add(EventVoteTimeSlotResponse.create(
                    voteTimeSlot,
                    participantCountByTimeSlot.getOrDefault(voteTimeSlot.getVoteTimeSlotId(), 0)
            ));
        }

        return responses;
    }

    // 사용자가 가능한 시간 슬롯을 선택한다.
    @Transactional
    public List<EventVoteTimeSlotResponse> selectTimeSlot(
            User user,
            Long voteId,
            EventVoteTimeSlotSelectRequest request
    ) {
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
            responses.add(EventVoteTimeSlotResponse.create(
                    voteTimeSlot,
                    countMap.getOrDefault(voteTimeSlot.getVoteTimeSlotId(), 0)
            ));
        }

        return responses;
    }

    // 투표 결과를 확정하고 일정으로 생성한다.
    @Transactional
    public EventDetailResponse createVoteResult(
            User user,
            Long voteId,
            EventVoteTimeSelectRequest request
    ) {
        Vote vote = getVoteById(voteId);
        validateVoteIsOpened(vote);
        TeamMember host = validateTeamMember(vote.getTeam(), user);

        List<VoteTimeSlot> selectedVoteTimeSlots = voteTimeService.getContinuousVoteTimeSlots(
                vote,
                request.getSelectedStartAt(),
                request.getSelectedEndAt()
        );

        List<TeamMember> availableTeamMembers = voteAvailabilityService.getAvailableTeamMembers(selectedVoteTimeSlots);

        List<User> receivers = voteParticipantService.getUsersByVoteExcluding(vote, user.getUserId());

        notificationService.createNotifications(
                receivers,
                "\"" + vote.getTitle() + "\" 일정이 확정됐어요",
                "확정된 일정을 캘린더에서 확인해보세요",
                NotificationType.TEAM_SCHEDULE,
                "/team/" + vote.getTeam().getTeamId() + "/vote/" + vote.getVoteId()
        );

        return voteResultService.createVoteResult(vote, host, availableTeamMembers, request);
    }

    // 팀을 조회한다.
    private Team getTeamById(
            Long teamId
    ) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_NOT_FOUND));
    }

    // 팀 멤버 여부를 검증한다.
    private TeamMember validateTeamMember(
            Team team,
            User user
    ) {
        return teamMemberRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TEAM_MEMBER_NOT_FOUND));
    }

    // 투표를 조회한다.
    private Vote getVoteById(
            Long voteId
    ) {
        return voteRepository.findById(voteId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.VOTE_NOT_FOUND));
    }

    // 투표 응답 DTO를 생성한다.
    private EventVoteResponse createEventVoteResponse(
            Vote vote
    ) {
        VoteParticipantService.VoteParticipants voteParticipants =
                voteParticipantService.getVoteParticipants(vote);

        List<LocalDate> dates = voteTimeService.getVoteDates(vote).stream()
                .map(VoteDate::getDate)
                .toList();

        return EventVoteResponse.create(
                vote,
                dates,
                voteParticipants.completedVoters(),
                voteParticipants.uncompletedVoters()
        );
    }

    // 투표가 열려 있는지 검증한다.
    private void validateVoteIsOpened(
            Vote vote
    ) {
        if (!Boolean.TRUE.equals(vote.getIsOpened())) {
            throw new RestApiException(CustomErrorCode.VOTE_NOT_OPENED);
        }
    }

}
