package com.inuteamflow.server.domain.admin.dto.response;

import com.inuteamflow.server.domain.report.entity.ReportHandle;
import com.inuteamflow.server.domain.report.enums.UserActionType;
import com.inuteamflow.server.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "정지/영구정지된 계정 목록 항목 DTO")
public class SuspendedUserResponse {

    @Schema(description = "사용자 ID", example = "11")
    private Long userId;

    @Schema(description = "로그인 아이디", example = "appcenter123")
    private String username;

    @Schema(description = "이름", example = "홍길동")
    private String name;

    @Schema(description = "제재 유형", example = "SUSPEND")
    private UserActionType action;

    @Schema(description = "정지 만료 시각 (영구정지면 null)")
    private LocalDateTime suspendedUntil;

    @Schema(description = "해제 요청 시 사용할 신고 ID (제재 근거를 찾지 못하면 null)")
    private Long reportId;

    @Schema(description = "조치 사유")
    private String reason;

    @Schema(description = "조치한 관리자 이름")
    private String handledBy;

    @Schema(description = "조치 시각")
    private LocalDateTime handledAt;

    public static SuspendedUserResponse of(User user, ReportHandle handle) {
        return new SuspendedUserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getName(),
                handle == null ? null : handle.getUserAction(),
                user.getSuspendedUntil(),
                handle == null ? null : handle.getReport().getReportId(),
                handle == null ? null : handle.getUserActionDetail(),
                handle == null ? null : handle.getHandlerName(),
                handle == null ? null : handle.getCreatedAt());
    }
}
