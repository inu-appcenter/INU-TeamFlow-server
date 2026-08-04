package com.inuteamflow.server.domain.notification.dto.res;

import com.inuteamflow.server.domain.notification.entity.Notification;
import com.inuteamflow.server.domain.notification.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "알림 항목 DTO")
public class NotificationItemResponse {

    @Schema(description = "알림 ID", example = "40")
    private Long notificationId;

    @Schema(description = "알림 제목", example = "[앱센터- 베이직 스터디 일정] 일정 투표가 시작되었습니다")
    private String title;

    @Schema(description = "알림 내용", example = "오프라인으로 진행될거구요. 가능하신 시간대 투표해주세요~")
    private String content;

    @Schema(description = "알림 유형", example = "CALENDAR")
    private NotificationType type;

    @Schema(description = "리다이렉트 URL", example = "/teams/1/notice/5")
    private String redirectUrl;

    @Schema(description = "읽음 여부", example = "false")
    private Boolean isRead;

    @Schema(description = "알림 생성 일시", example = "2026-07-10T09:00:00")
    private LocalDateTime createdAt;

    public static NotificationItemResponse create(Notification notification) {
        return new NotificationItemResponse(
                notification.getNotificationId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getType(),
                notification.getRedirectUrl(),
                notification.getIsRead(),
                notification.getCreatedAt());
    }
}
