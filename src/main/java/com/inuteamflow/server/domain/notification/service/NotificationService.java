package com.inuteamflow.server.domain.notification.service;

import com.inuteamflow.server.domain.fcm.dto.ChatFcmEvent;
import com.inuteamflow.server.domain.fcm.dto.FcmMultiEvent;
import com.inuteamflow.server.domain.fcm.dto.FcmSingleEvent;
import com.inuteamflow.server.domain.notification.dto.req.NotificationRequest;
import com.inuteamflow.server.domain.notification.dto.res.NotificationSliceResponse;
import com.inuteamflow.server.domain.notification.entity.Notification;
import com.inuteamflow.server.domain.notification.enums.NotificationType;
import com.inuteamflow.server.domain.notification.repository.NotificationRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final ApplicationEventPublisher eventPublisher;
    private final NotificationRepository notificationRepository;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 사용자의 알림 목록을 조회한다.
     *
     * <p>알림 유형이 지정되면 해당 유형만 조회하고, 유형과 관계없이 사용자의 전체 읽지 않은 알림 수를 함께 반환한다.</p>
     *
     * @param type 조회할 알림 유형 또는 전체 유형이면 {@code null}
     * @param user 알림 수신자
     * @param pageable 알림 목록의 페이지 정보
     * @return 다음 페이지 여부와 읽지 않은 알림 수를 포함한 알림 목록
     */
    public NotificationSliceResponse getNotifications(NotificationType type, User user, Pageable pageable) {
        Slice<Notification> slice = (type != null)
                ? notificationRepository.findByReceiverAndType(user, type, pageable)
                : notificationRepository.findByReceiver(user, pageable);

        Integer unreadCount = notificationRepository.countByReceiverAndIsReadFalse(user);

        return NotificationSliceResponse.create(slice, unreadCount);
    }

    /**
     * 사용자의 알림 한 건을 읽음 처리한다.
     *
     * @param notificationId 읽음 처리할 알림 ID
     * @param user 알림을 읽는 사용자
     * @throws RestApiException 알림을 찾을 수 없거나 사용자가 알림 수신자가 아닌 경우
     */
    @Transactional
    public void readNotification(Long notificationId, User user) {
        Notification notification = notificationRepository
                .findByIdWithReceiver(notificationId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getReceiver().getUserId().equals(user.getUserId())) {
            throw new RestApiException(CustomErrorCode.NOTIFICATION_FORBIDDEN);
        }

        notification.read();
    }

    /**
     * 사용자의 여러 알림을 읽음 처리한다.
     *
     * @param request 읽음 처리할 알림 ID 목록
     * @param user 알림을 읽는 사용자
     */
    @Transactional
    public void readNotifications(NotificationRequest request, User user) {
        notificationRepository.bulkRead(request.getNotificationIds(), user);
    }

    /**
     * 사용자의 여러 알림을 삭제한다.
     *
     * @param request 삭제할 알림 ID 목록
     * @param user 알림을 삭제하는 사용자
     */
    @Transactional
    public void deleteNotifications(NotificationRequest request, User user) {
        notificationRepository.bulkDelete(request.getNotificationIds(), user);
    }

    /**
     * 단일 수신자의 알림을 생성한다.
     *
     * <p>알림을 저장한 뒤 트랜잭션 커밋 이후 발송할 {@link FcmSingleEvent}를 발행한다.</p>
     *
     * @param receiver 알림 수신자
     * @param title 알림 제목
     * @param content 알림 내용
     * @param type 알림 유형
     * @param redirectUrl 알림 선택 시 이동할 URL
     */
    @Transactional
    public void createNotification(
            User receiver, String title, String content, NotificationType type, String redirectUrl) {
        Notification notification =
                notificationRepository.save(Notification.create(receiver, title, content, type, redirectUrl));
        eventPublisher.publishEvent(new FcmSingleEvent(
                receiver.getUserId(), title, content, redirectUrl, type, notification.getNotificationId()));
    }

    /**
     * 여러 수신자의 알림을 생성한다.
     *
     * <p>수신자별 알림을 일괄 저장한 뒤 트랜잭션 커밋 이후 발송할 {@link FcmMultiEvent}를 한 번 발행한다.</p>
     *
     * @param receivers 알림 수신자 목록
     * @param title 알림 제목
     * @param content 알림 내용
     * @param type 알림 유형
     * @param redirectUrl 알림 선택 시 이동할 URL
     */
    @Transactional
    public void createNotifications(
            List<User> receivers, String title, String content, NotificationType type, String redirectUrl) {
        List<Notification> notifications = receivers.stream()
                .map(receiver -> Notification.create(receiver, title, content, type, redirectUrl))
                .toList();
        notificationRepository.saveAll(notifications);
        List<Long> receiverIds = receivers.stream().map(User::getUserId).toList();
        eventPublisher.publishEvent(new FcmMultiEvent(receiverIds, title, content, redirectUrl, type));
    }

    /**
     * 여러 수신자에게 채팅 FCM 이벤트를 발행한다.
     *
     * <p>채팅방 ID로 축약 키를 생성하여 같은 채팅방의 알림을 하나의 그룹으로 처리하도록 한다.</p>
     *
     * @param receiverIds 알림 수신자 ID 목록
     * @param title 알림 제목
     * @param content 알림 내용
     * @param type 알림 유형
     * @param redirectUrl 알림 선택 시 이동할 URL
     * @param roomId 채팅방 ID
     */
    public void sendChatFcm(
            List<Long> receiverIds,
            String title,
            String content,
            NotificationType type,
            String redirectUrl,
            Long roomId) {
        eventPublisher.publishEvent(
                new ChatFcmEvent(receiverIds, title, content, type, redirectUrl, roomId, "chat-room-" + roomId));
    }
}
