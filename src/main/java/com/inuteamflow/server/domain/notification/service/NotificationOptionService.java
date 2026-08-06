package com.inuteamflow.server.domain.notification.service;

import com.inuteamflow.server.domain.notification.dto.req.NotificationOptionRequest;
import com.inuteamflow.server.domain.notification.dto.res.NotificationOptionResponse;
import com.inuteamflow.server.domain.notification.entity.NotificationOption;
import com.inuteamflow.server.domain.notification.repository.NotificationOptionRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationOptionService {

    private final NotificationOptionRepository notificationOptionRepository;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 사용자의 알림 활성화 옵션을 조회한다.
     *
     * @param user 알림 옵션을 조회할 사용자
     * @return 사용자의 알림 유형별 활성화 여부
     * @throws RestApiException 알림 옵션을 찾을 수 없는 경우
     */
    public NotificationOptionResponse getNotificationOption(User user) {
        NotificationOption option = notificationOptionRepository
                .findByUser(user)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.NOTIFICATION_OPTION_NOT_FOUND));
        return NotificationOptionResponse.from(option);
    }

    /**
     * 사용자의 알림 활성화 옵션을 생성한다.
     *
     * <p>회원가입 시에 요청되어야 한다.</p>
     *
     * @param request 알림 유형별 활성화 여부
     * @param user 알림 옵션을 생성할 사용자
     * @return 생성된 사용자의 알림 유형별 활성화 여부
     * @throws RestApiException 이미 알림 옵션이 존재하는 경우
     */
    @Transactional
    public NotificationOptionResponse createNotificationOption(NotificationOptionRequest request, User user) {
        if (notificationOptionRepository.existsByUser(user)) {
            throw new RestApiException(CustomErrorCode.NOTIFICATION_OPTION_ALREADY_EXISTS);
        }
        NotificationOption option = NotificationOption.create(request, user);
        notificationOptionRepository.save(option);
        return NotificationOptionResponse.from(option);
    }

    /**
     * 사용자의 알림 활성화 옵션을 수정한다.
     *
     * @param request 수정할 알림 유형별 활성화 여부
     * @param user 알림 옵션을 수정할 사용자
     * @return 수정된 사용자의 알림 유형별 활성화 여부
     * @throws RestApiException 알림 옵션을 찾을 수 없는 경우
     */
    @Transactional
    public NotificationOptionResponse updateNotificationOption(NotificationOptionRequest request, User user) {
        NotificationOption option = notificationOptionRepository
                .findByUser(user)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.NOTIFICATION_OPTION_NOT_FOUND));
        option.update(request);
        return NotificationOptionResponse.from(option);
    }
}
