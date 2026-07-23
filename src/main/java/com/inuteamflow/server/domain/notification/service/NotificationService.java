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
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final ApplicationEventPublisher eventPublisher;
    private final NotificationRepository notificationRepository;

    public NotificationSliceResponse getNotifications(
            NotificationType type,
            User user,
            Pageable pageable
    ) {
        // type에 따라서 조회
        Slice<Notification> slice = (type != null)
                ? notificationRepository.findByReceiverAndType(user, type, pageable)
                : notificationRepository.findByReceiver(user, pageable);

        // 사용자가 읽지 않은 알림의 개수
        Integer unreadCount = notificationRepository.countByReceiverAndIsReadFalse(user);

        // hasNext + unreadCount + notifications로 반환
        return NotificationSliceResponse.create(slice, unreadCount);
    }

    @Transactional
    public void readNotification(
            Long notificationId,
            User user
    ) {
        // 알림 ID와 사용자(수신자)의 조합으로 조회
        // findById로 가져오면 이후 notification.getReceiver().getUserId()를 호출하는 순간 추가 쿼리가 발생함
        Notification notification = notificationRepository.findByIdWithReceiver(notificationId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.NOTIFICATION_NOT_FOUND));

        // 사용자와 수신자가 다르다면 에러
        if (!notification.getReceiver().getUserId().equals(user.getUserId())) {
            throw new RestApiException(CustomErrorCode.NOTIFICATION_FORBIDDEN);
        }

        // 읽음 처리
        notification.read();
    }

    @Transactional
    public void readNotifications(
            NotificationRequest request,
            User user
    ) {
        // 벌크 연산으로 레파지토리 레이어에서 읽음 처리
        notificationRepository.bulkRead(request.getNotificationIds(), user);
    }

    @Transactional
    public void deleteNotifications(
            NotificationRequest request,
            User user
    ) {
        // 벌크 연산으로 레파지토리 레이어에서 삭제 처리
        notificationRepository.bulkDelete(request.getNotificationIds(), user);
    }

    @Transactional
    public void createNotification(
            User receiver,
            String title,
            String content,
            NotificationType type,
            String redirectUrl
    ) {
        Notification notification = notificationRepository.save(Notification.create(receiver, title, content, type, redirectUrl));
        eventPublisher.publishEvent(
                new FcmSingleEvent(
                        receiver.getUserId(),
                        title,
                        content,
                        redirectUrl,
                        type,
                        notification.getNotificationId()
                )
        );
    }

    @Transactional
    public void createNotifications(
            List<User> receivers,
            String title,
            String content,
            NotificationType type,
            String redirectUrl
    ) {
        List<Notification> notifications = receivers.stream()
                .map(receiver -> Notification.create(receiver, title, content, type, redirectUrl))
                .toList();
        notificationRepository.saveAll(notifications);
        List<Long> receiverIds = receivers.stream().map(User::getUserId).toList();
        eventPublisher.publishEvent(
                new FcmMultiEvent(
                        receiverIds,
                        title,
                        content,
                        redirectUrl,
                        type
                )
        );
    }

    public void sendChatFcm(
            List<Long> receiverIds,
            String title,
            String content,
            NotificationType type,
            String redirectUrl,
            Long roomId
    ) {
        eventPublisher.publishEvent(
                new ChatFcmEvent(
                        receiverIds,
                        title,
                        content,
                        type,
                        redirectUrl,
                        roomId,
                        "chat-room-" + roomId
                )
        );
    }

}
