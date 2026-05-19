package com.inuteamflow.server.domain.vote.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import com.inuteamflow.server.domain.vote.entity.Vote;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EventVoteResponse {

    private Long voteId;
    private Long teamId;
    private String title;
    private String description;
    private LocalDateTime date;
    private Boolean isOpened;

    private Boolean isAllDay;
    private LocalTime dailyTimeStart;
    private LocalTime dailyTimeEnd;

    private List<String> completedVoterNameList;
    private List<String> uncompletedVoterNameList;

    public static EventVoteResponse create(
            Vote vote,
            List<String> completedVoterNameList,
            List<String> uncompletedVoterNameList
    ) {
        return new EventVoteResponse(
                vote.getVoteId(),
                vote.getTeam().getTeamId(),
                vote.getTitle(),
                vote.getDescription(),
                vote.getCreatedAt(),
                vote.getIsOpened(),
                vote.getIsAllDay(),
                vote.getDailyTimeStart(),
                vote.getDailyTimeEnd(),
                completedVoterNameList,
                uncompletedVoterNameList
        );
    }
}
