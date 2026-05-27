package com.inuteamflow.server.domain.vote.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import com.inuteamflow.server.domain.vote.entity.Vote;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "일정 투표 응답 DTO")
public class EventVoteResponse {

    @Schema(description = "투표 ID", example = "1")
    private Long voteId;

    @Schema(description = "팀 ID", example = "10")
    private Long teamId;

    @Schema(description = "투표 제목", example = "스터디 일정 조율")
    private String title;

    @Schema(description = "투표 설명", example = "다음주 스터디 가능한 시간을 선택해주세요.")
    private String description;

    @Schema(description = "투표 생성 날짜", example = "2026-05-19")
    private LocalDate createdDate;

    @Schema(description = "투표 진행 여부", example = "true")
    private Boolean isOpened;

    @Schema(description = "종일 여부", example = "false")
    private Boolean isAllDay;

    @Schema(description = "지정된 투표 날짜", example = "[\"2026-05-20\", \"2026-05-21\", \"2026-05-25\"]")
    private List<LocalDate> dates;

    @Schema(description = "일일 시작 시간", example = "09:00:00")
    private LocalTime dailyTimeStart;

    @Schema(description = "일일 종료 시간", example = "20:00:00")
    private LocalTime dailyTimeEnd;

    @Schema(description = "투표 완료 유저 이름 목록", example = "[\"홍길동\", \"김철수\"]")
    private List<String> completedVoterNameList;

    @Schema(description = "투표 미완료 유저 이름 목록", example = "[\"이영희\"]")
    private List<String> uncompletedVoterNameList;

    public static EventVoteResponse create(
            Vote vote,
            List<LocalDate> dates,
            List<String> completedVoterNameList,
            List<String> uncompletedVoterNameList
    ) {
        return new EventVoteResponse(
                vote.getVoteId(),
                vote.getTeam().getTeamId(),
                vote.getTitle(),
                vote.getDescription(),
                vote.getCreatedAt().toLocalDate(),
                vote.getIsOpened(),
                vote.getIsAllDay(),
                dates,
                vote.getDailyTimeStart(),
                vote.getDailyTimeEnd(),
                completedVoterNameList,
                uncompletedVoterNameList
        );
    }
}
