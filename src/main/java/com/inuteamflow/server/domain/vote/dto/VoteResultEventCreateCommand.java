package com.inuteamflow.server.domain.vote.dto;

import com.inuteamflow.server.domain.event.dto.EventCreateCommand;
import com.inuteamflow.server.domain.event.dto.Recurrence;
import com.inuteamflow.server.domain.event.enums.EventColor;
import com.inuteamflow.server.domain.vote.dto.request.EventVoteTimeSelectRequest;
import com.inuteamflow.server.domain.vote.entity.Vote;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class VoteResultEventCreateCommand implements EventCreateCommand {

    private final Vote vote;
    private final EventVoteTimeSelectRequest request;

    @Override
    public String getTitle() {
        return request.getTitle();
    }

    @Override
    public String getDescription() {
        return vote.getDescription();
    }

    @Override
    public LocalDateTime getStartAt() {
        return request.getSelectedStartAt();
    }

    @Override
    public LocalDateTime getEndAt() {
        return request.getSelectedEndAt();
    }

    @Override
    public Boolean getIsAllDay() {
        return request.getIsAllDay();
    }

    @Override
    public EventColor getColor() {
        return EventColor.OCEAN;
    }

    @Override
    public Recurrence getRecurrence() {
        return null;
    }
}
