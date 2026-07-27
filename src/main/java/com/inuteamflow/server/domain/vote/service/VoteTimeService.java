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
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteTimeService {

    private final VoteDateRepository voteDateRepository;
    private final VoteTimeSlotRepository voteTimeSlotRepository;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 투표의 후보 날짜를 생성한다.
     *
     * <p>중복된 날짜는 제거하고 날짜 오름차순으로 저장한다.</p>
     *
     * @param vote 후보 날짜를 등록할 투표
     * @param voteDates 등록할 후보 날짜 목록
     * @return 저장된 후보 날짜 목록
     */
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

    /**
     * 각 후보 날짜에 투표 가능한 시간 슬롯을 생성한다.
     *
     * <p>종일 투표는 날짜마다 하나의 종일 슬롯을 생성하고, 시간 지정 투표는
     * 투표의 슬롯 단위에 따라 요청된 일일 시간 범위를 분할한다.</p>
     *
     * @param voteDates 시간 슬롯을 생성할 후보 날짜 목록
     * @param request 종일 여부와 일일 시간 범위를 포함한 투표 생성 요청
     */
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

    /**
     * 특정 후보 날짜의 시간 범위를 투표 단위에 맞는 슬롯으로 분할한다.
     *
     * <p>종일 투표인 경우 해당 날짜에 하나의 종일 슬롯을 생성한다.</p>
     *
     * @param voteDate 시간 슬롯을 생성할 후보 날짜
     * @param request 종일 여부와 일일 시간 범위를 포함한 투표 생성 요청
     * @return 생성된 시간 슬롯 목록
     */
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

    /**
     * 투표의 후보 날짜를 오름차순으로 조회한다.
     *
     * @param vote 후보 날짜를 조회할 투표
     * @return 날짜 오름차순으로 정렬된 후보 날짜 목록
     */
    public List<VoteDate> getVoteDates(
            Vote vote
    ) {
        return voteDateRepository.findByVoteOrderByDateAsc(vote);
    }

    /**
     * 투표의 모든 후보 시간 슬롯을 날짜와 시작 시간 순으로 조회한다.
     *
     * @param vote 시간 슬롯을 조회할 투표
     * @return 날짜와 시작 시간 순으로 정렬된 후보 시간 슬롯 목록
     */
    public List<VoteTimeSlot> getVoteTimeSlots(
            Vote vote
    ) {
        List<VoteDate> voteDates = getVoteDates(vote);

        if (voteDates.isEmpty()) {
            return List.of();
        }

        return voteTimeSlotRepository.findByVoteDatesOrderByDateAndStartAt(voteDates);
    }

    /**
     * 요청된 ID가 모두 해당 투표에 속하는지 검증하고 시간 슬롯을 조회한다.
     *
     * <p>중복된 ID는 하나로 처리한다.</p>
     *
     * @param vote 시간 슬롯이 속해야 하는 투표
     * @param voteTimeSlotIds 조회할 시간 슬롯 ID 목록
     * @return 검증된 시간 슬롯 목록
     * @throws RestApiException ID 목록이 비어 있거나 해당 투표에 속하지 않는
     *                          시간 슬롯 ID가 포함된 경우
     */
    public List<VoteTimeSlot> getValidVoteTimeSlots(
            Vote vote,
            List<Long> voteTimeSlotIds
    ) {
        if (voteTimeSlotIds == null || voteTimeSlotIds.isEmpty()) {
            throw new RestApiException(CustomErrorCode.VOTE_TIME_SLOT_INVALID);
        }

        List<Long> uniqueIds = voteTimeSlotIds.stream()
                .filter(id -> id != null)
                .distinct()
                .toList();

        if (uniqueIds.isEmpty()) {
            throw new RestApiException(CustomErrorCode.VOTE_TIME_SLOT_INVALID);
        }

        List<VoteTimeSlot> voteTimeSlots = voteTimeSlotRepository.findByIdsAndVote(uniqueIds, vote);

        if (voteTimeSlots.size() != uniqueIds.size()) {
            throw new RestApiException(CustomErrorCode.VOTE_TIME_SLOT_INVALID);
        }

        return voteTimeSlots;
    }

    /**
     * 선택한 시작 시각부터 종료 시각까지 연속된 시간 슬롯을 조회한다.
     *
     * <p>시작과 종료 시각은 같은 날짜여야 하며, 조회된 슬롯은 선택 범위의
     * 시작과 종료에 정확히 일치하고 중간에 빈 구간이 없어야 한다.</p>
     *
     * @param vote 시간 슬롯을 조회할 투표
     * @param selectedStartAt 선택한 시작 일시
     * @param selectedEndAt 선택한 종료 일시
     * @return 선택한 범위를 구성하는 연속된 시간 슬롯 목록
     * @throws RestApiException 시작·종료 일시 또는 해당 범위의 시간 슬롯이 유효하지 않은 경우
     */
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

    /**
     * 투표의 모든 시간 슬롯과 후보 날짜를 삭제한다.
     *
     * @param voteId 시간 정보를 삭제할 투표 ID
     */
    @Transactional
    public void deleteByVoteId(Long voteId) {
        voteTimeSlotRepository.deleteByVoteId(voteId);
        voteDateRepository.deleteByVoteId(voteId);
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 선택한 시작 및 종료 일시가 유효한 범위인지 검증한다.
     *
     * <p>두 일시는 같은 날짜여야 하며 시작 일시가 종료 일시보다 앞서야 한다.</p>
     *
     * @param selectedStartAt 선택한 시작 일시
     * @param selectedEndAt 선택한 종료 일시
     * @throws RestApiException 일시가 누락되었거나 날짜 또는 시간 범위가 유효하지 않은 경우
     */
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

    /**
     * 조회된 시간 슬롯이 선택한 시간 범위를 빈 구간 없이 구성하는지 검증한다.
     *
     * @param voteTimeSlots 연속성을 검증할 시간 슬롯 목록
     * @param selectedStartTime 선택한 시작 시각
     * @param selectedEndTime 선택한 종료 시각
     * @throws RestApiException 시간 슬롯이 없거나 선택 범위와 일치하지 않는 경우,
     *                          또는 시간 슬롯 사이에 빈 구간이 있는 경우
     */
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

}
