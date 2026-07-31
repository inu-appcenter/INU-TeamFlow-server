package com.inuteamflow.server.domain.vote.dto.response;

import com.inuteamflow.server.domain.user.enums.Department;
import com.inuteamflow.server.domain.vote.entity.VoteParticipant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "투표자 정보 응답 DTO")
public class VoterInfoResponse {

    @Schema(description = "이름", example = "홍길동")
    private String name;

    @Schema(description = "학과", example = "COMPUTER_SCIENCE")
    private Department department;

    public static VoterInfoResponse create(VoteParticipant voteParticipant) {
        return new VoterInfoResponse(
                voteParticipant.getTeamMember().getUser().getName(),
                voteParticipant.getTeamMember().getUser().getDepartment());
    }
}
