package com.inuteamflow.server.domain.report.dto.request;

import com.inuteamflow.server.domain.report.enums.PostActionType;
import com.inuteamflow.server.domain.report.enums.UserActionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReportHandleRequest {

    /** USER 신고면 null. */
    @Valid
    private PostActionCommand postAction;

    @NotNull
    @Valid
    private UserActionCommand userAction;

    @Getter
    @NoArgsConstructor
    public static class PostActionCommand {

        @NotNull
        private PostActionType action;

        @Size(max = 1000)
        private String detail;
    }

    @Getter
    @NoArgsConstructor
    public static class UserActionCommand {

        @NotNull
        private UserActionType action;

        /** action=SUSPEND일 때만 필수 (서비스에서 검증). */
        private Integer durationDays;

        @Size(max = 1000)
        private String detail;
    }
}
