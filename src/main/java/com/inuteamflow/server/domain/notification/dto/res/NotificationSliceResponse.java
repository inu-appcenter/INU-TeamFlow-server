package com.inuteamflow.server.domain.notification.dto.res;

import com.inuteamflow.server.domain.notification.entity.Notification;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Slice;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "알림 목록(무한스크롤) 응답 DTO")
public class NotificationSliceResponse {

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private Boolean hasNext;

    @Schema(description = "읽지 않은 알림 수", example = "8")
    private Integer unreadCount;

    @Schema(description = "알림 목록")
    private List<NotificationItemResponse> notifications;

    public static NotificationSliceResponse of(Slice<Notification> slice, Integer unreadCount) {
        return new NotificationSliceResponse(
                slice.hasNext(),
                unreadCount,
                slice.getContent().stream().map(NotificationItemResponse::from).toList());
    }
}
