package com.inuteamflow.server.domain.notification.dto.res;

import com.inuteamflow.server.domain.notification.entity.NotificationOption;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "알림 활성화 옵션 DTO")
public class NotificationOptionResponse {

    @Schema(description = "전체 알림 활성화 여부", example = "true")
    private Boolean allEnabled;

    @Schema(description = "공지 알림 활성화 여부", example = "true")
    private Boolean noticeEnabled;

    @Schema(description = "초대 알림 활성화 여부", example = "true")
    private Boolean inviteEnabled;

    @Schema(description = "신청 알림 활성화 여부", example = "true")
    private Boolean applicationEnabled;

    @Schema(description = "일정 알림 활성화 여부", example = "true")
    private Boolean calendarEnabled;

    @Schema(description = "채팅 알림 활성화 여부", example = "true")
    private Boolean chatEnabled;

    public static NotificationOptionResponse from(NotificationOption option) {
        return new NotificationOptionResponse(
                isAllEnabled(option),
                option.getNoticeEnabled(),
                option.getInviteEnabled(),
                option.getApplicationEnabled(),
                option.getCalendarEnabled(),
                option.getChatEnabled());
    }

    private static Boolean isAllEnabled(NotificationOption option) {
        return option.getNoticeEnabled()
                && option.getInviteEnabled()
                && option.getApplicationEnabled()
                && option.getCalendarEnabled()
                && option.getChatEnabled();
    }
}
