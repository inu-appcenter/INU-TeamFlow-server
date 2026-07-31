package com.inuteamflow.server.domain.notification.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "알림 선택 읽음/삭제 요청 DTO")
public class NotificationRequest {

    @NotEmpty
    @Schema(description = "선택 읽음/삭제를 적용할 알림 ID 목록", example = "[1, 3, 7, 20]")
    private List<Long> notificationIds;
}
