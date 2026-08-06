package com.inuteamflow.server.domain.notification.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "알림 활성화 옵션 생성/수정 요청 DTO")
public class NotificationOptionRequest {

    @NotNull
    @Schema(description = "공지 알림 활성화 여부", example = "true")
    private Boolean noticeEnabled;

    @NotNull
    @Schema(description = "초대 알림 활성화 여부", example = "true")
    private Boolean inviteEnabled;

    @NotNull
    @Schema(description = "신청 알림 활성화 여부", example = "true")
    private Boolean applicationEnabled;

    @NotNull
    @Schema(description = "일정 알림 활성화 여부", example = "true")
    private Boolean calendarEnabled;

    @NotNull
    @Schema(description = "채팅 알림 활성화 여부", example = "true")
    private Boolean chatEnabled;
}
