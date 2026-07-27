package com.inuteamflow.server.domain.invitation.dto.response;

import com.inuteamflow.server.domain.invitation.enums.InvitationCandidateStatus;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.enums.Department;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "초대 가능한 사용자 응답 DTO")
public class InvitationCandidateResponse {

    @Schema(description = "유저 ID", example = "1")
    private Long userId;

    @Schema(description = "이름", example = "손동민")
    private String name;

    @Schema(description = "학번", example = "202201486")
    private String studentNumber;

    @Schema(description = "학과", example = "COMPUTER_SCIENCE")
    private Department department;

    @Schema(description = "초대 상태", example = "PENDING")
    private InvitationCandidateStatus invitationStatus;

    public static InvitationCandidateResponse from(
            User user,
            InvitationCandidateStatus invitationStatus
    ) {
        return new InvitationCandidateResponse(
                user.getUserId(),
                user.getName(),
                user.getStudentNumber(),
                user.getDepartment(),
                invitationStatus
        );
    }
}
