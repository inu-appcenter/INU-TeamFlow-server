package com.inuteamflow.server.domain.vote.service;

import com.inuteamflow.server.domain.vote.dto.request.EventVoteCreateRequest;
import com.inuteamflow.server.domain.vote.dto.response.EventVoteTimeSlotResponse;
import com.inuteamflow.server.domain.vote.entity.Vote;
import com.inuteamflow.server.domain.vote.entity.VoteDate;
import com.inuteamflow.server.domain.vote.entity.VoteTimeSlot;
import com.inuteamflow.server.domain.vote.repository.VoteDateRepository;
import com.inuteamflow.server.domain.vote.repository.VoteTimeSlotRepository;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteTimeService {

    private final VoteDateRepository voteDateRepository;
    private final VoteTimeSlotRepository voteTimeSlotRepository;

    // 투표 가능한 날짜 목록을 생성한다.
    @Transactional
    public List<VoteDate> createVoteDates(
            Vote vote,
            List<LocalDate> voteDates
    ) {
        List<VoteDate> voteDateEntities = voteDates.stream()
                .distinct()
                .sorted()
                .map(date -> VoteDate.create(vote, date))
                .toList();

        return voteDateRepository.saveAll(voteDateEntities);
    }

    // 투표 날짜별 시간 슬롯을 생성한다.
    @Transactional
    public void createVoteTimeSlots(
            List<VoteDate> voteDates,
            EventVoteCreateRequest request
    ) {
        List<VoteTimeSlot> voteTimeSlots = new ArrayList<>();

        for (VoteDate voteDate : voteDates) {
            voteTimeSlots.addAll(createVoteTimeSlots(voteDate, request));
        }

        voteTimeSlotRepository.saveAll(voteTimeSlots);
    }

    // 투표 날짜 목록을 조회한다.
    public List<VoteDate> getVoteDates(
            Vote vote
    ) {
        return voteDateRepository.findByVoteOrderByDateAsc(vote);
    }

    // 투표 시간 슬롯 목록을 조회한다.
    public List<VoteTimeSlot> getVoteTimeSlots(
            Vote vote
    ) {
        List<VoteDate> voteDates = getVoteDates(vote);

        if (voteDates.isEmpty()) {
            return List.of();
        }

        return voteTimeSlotRepository.findByVoteDatesOrderByDateAndStartAt(voteDates);
    }

    // 시간 슬롯 ID 목록으로 시간 슬롯을 조회한다.
    public List<VoteTimeSlot> getVoteTimeSlotsByIds(
            List<Long> voteTimeSlotIds
    ) {
        if (voteTimeSlotIds == null || voteTimeSlotIds.isEmpty()) {
            return List.of();
        }

        return voteTimeSlotRepository.findAllById(voteTimeSlotIds);
    }

    // 선택 가능한 유효한 시간 슬롯 목록을 조회한다.
    public List<VoteTimeSlot> getValidVoteTimeSlots(
            Vote vote,
            List<Long> voteTimeSlotIds
    ) {
        List<VoteTimeSlot> voteTimeSlots = getVoteTimeSlotsByIds(voteTimeSlotIds);
        validateSelectedVoteTimeSlots(voteTimeSlotIds, voteTimeSlots);
        validateVoteTimeSlots(vote, voteTimeSlots);

        return voteTimeSlots;
    }

    // 연속된 시간 슬롯들을 조회한다.
    public List<VoteTimeSlot> getContinuousVoteTimeSlots(
            Vote vote,
            LocalDateTime selectedStartAt,
            LocalDateTime selectedEndAt
    ) {
        validateSelectedDateTimeRange(selectedStartAt, selectedEndAt);

        List<VoteTimeSlot> voteTimeSlots = voteTimeSlotRepository.findByVoteAndDateAndTimeRange(
                vote,
                selectedStartAt.toLocalDate(),
                selectedStartAt.toLocalTime(),
                selectedEndAt.toLocalTime()
        );
        validateContinuousVoteTimeSlots(voteTimeSlots, selectedStartAt.toLocalTime(), selectedEndAt.toLocalTime());

        return voteTimeSlots;
    }

    // 시간 슬롯들이 해당 투표에 속하는지 검증한다.
    public void validateVoteTimeSlots(
            Vote vote,
            List<VoteTimeSlot> voteTimeSlots
    ) {
        if (voteTimeSlots == null || voteTimeSlots.isEmpty()) {
            throw new RestApiException(CustomErrorCode.VOTE_TIME_SLOT_INVALID);
        }

        for (VoteTimeSlot voteTimeSlot : voteTimeSlots) {
            if (!vote.getVoteId().equals(voteTimeSlot.getVoteDate().getVote().getVoteId())) {
                throw new RestApiException(CustomErrorCode.VOTE_TIME_SLOT_NOT_FOUND);
            }
        }
    }

    // 선택한 시간 슬롯 ID 목록의 유효성을 검증한다.
    private void validateSelectedVoteTimeSlots(
            List<Long> voteTimeSlotIds,
            List<VoteTimeSlot> voteTimeSlots
    ) {
        if (voteTimeSlotIds == null) {
            throw new RestApiException(CustomErrorCode.VOTE_TIME_SLOT_INVALID);
        }

        Set<Long> uniqueVoteTimeSlotIds = new HashSet<>();

        for (Long voteTimeSlotId : voteTimeSlotIds) {
            if (voteTimeSlotId == null) {
                continue;
            }

            uniqueVoteTimeSlotIds.add(voteTimeSlotId);
        }

        int requestedVoteTimeSlotCount = uniqueVoteTimeSlotIds.size();

        if (requestedVoteTimeSlotCount == 0 || requestedVoteTimeSlotCount != voteTimeSlots.size()) {
            throw new RestApiException(CustomErrorCode.VOTE_TIME_SLOT_INVALID);
        }
    }

    private void validateSelectedDateTimeRange(
            LocalDateTime selectedStartAt,
            LocalDateTime selectedEndAt
    ) {
        if (selectedStartAt == null || selectedEndAt == null) {
            throw new RestApiException(CustomErrorCode.VOTE_DATE_INVALID);
        }

        if (!selectedStartAt.toLocalDate().equals(selectedEndAt.toLocalDate())) {
            throw new RestApiException(CustomErrorCode.VOTE_DATE_INVALID);
        }

        if (!selectedStartAt.isBefore(selectedEndAt)) {
            throw new RestApiException(CustomErrorCode.VOTE_DATE_INVALID);
        }
    }

    private void validateContinuousVoteTimeSlots(
            List<VoteTimeSlot> voteTimeSlots,
            LocalTime selectedStartTime,
            LocalTime selectedEndTime
    ) {
        if (voteTimeSlots.isEmpty()) {
            throw new RestApiException(CustomErrorCode.VOTE_TIME_SLOT_INVALID);
        }

        if (!voteTimeSlots.get(0).getSlotStartAt().equals(selectedStartTime)) {
            throw new RestApiException(CustomErrorCode.VOTE_TIME_SLOT_INVALID);
        }

        VoteTimeSlot previousVoteTimeSlot = null;

        for (VoteTimeSlot voteTimeSlot : voteTimeSlots) {
            if (previousVoteTimeSlot != null
                    && !previousVoteTimeSlot.getSlotEndAt().equals(voteTimeSlot.getSlotStartAt())) {
                throw new RestApiException(CustomErrorCode.VOTE_TIME_SLOT_INVALID);
            }

            previousVoteTimeSlot = voteTimeSlot;
        }

        if (!previousVoteTimeSlot.getSlotEndAt().equals(selectedEndTime)) {
            throw new RestApiException(CustomErrorCode.VOTE_TIME_SLOT_INVALID);
        }
    }

    private List<VoteTimeSlot> createVoteTimeSlots(
            VoteDate voteDate,
            EventVoteCreateRequest request
    ) {
        if (Boolean.TRUE.equals(request.getIsAllDay())) {
            return List.of(VoteTimeSlot.create(voteDate, LocalTime.MIN, LocalTime.MAX));
        }

        List<VoteTimeSlot> voteTimeSlots = new ArrayList<>();
        LocalTime slotStartAt = request.getDailyTimeStart();

        while (slotStartAt.isBefore(request.getDailyTimeEnd())) {
            LocalTime slotEndAt = slotStartAt.plusMinutes(voteDate.getVote().getSlotUnitMinute());

            if (slotEndAt.isAfter(request.getDailyTimeEnd())) break;

            voteTimeSlots.add(VoteTimeSlot.create(voteDate, slotStartAt, slotEndAt));
            slotStartAt = slotEndAt;
        }

        return voteTimeSlots;
    }
}
