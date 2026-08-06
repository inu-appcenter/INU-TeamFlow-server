package com.inuteamflow.server.domain.notification.entity;

import com.inuteamflow.server.domain.notification.dto.req.NotificationOptionRequest;
import com.inuteamflow.server.domain.notification.enums.NotificationType;
import com.inuteamflow.server.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "notification_option")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_option_id")
    private Long notificationOptionId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "notice_enabled", nullable = false)
    private Boolean noticeEnabled;

    @Column(name = "invite_enabled", nullable = false)
    private Boolean inviteEnabled;

    @Column(name = "application_enabled", nullable = false)
    private Boolean applicationEnabled;

    @Column(name = "calendar_enabled", nullable = false)
    private Boolean calendarEnabled;

    @Column(name = "chat_enabled", nullable = false)
    private Boolean chatEnabled;

    @Builder
    private NotificationOption(
            User user,
            Boolean noticeEnabled,
            Boolean inviteEnabled,
            Boolean applicationEnabled,
            Boolean calendarEnabled,
            Boolean chatEnabled) {
        this.user = user;
        this.noticeEnabled = noticeEnabled;
        this.inviteEnabled = inviteEnabled;
        this.applicationEnabled = applicationEnabled;
        this.calendarEnabled = calendarEnabled;
        this.chatEnabled = chatEnabled;
    }

    public static NotificationOption create(NotificationOptionRequest request, User user) {
        return NotificationOption.builder()
                .user(user)
                .noticeEnabled(request.getNoticeEnabled())
                .inviteEnabled(request.getInviteEnabled())
                .applicationEnabled(request.getApplicationEnabled())
                .calendarEnabled(request.getCalendarEnabled())
                .chatEnabled(request.getChatEnabled())
                .build();
    }

    public void update(NotificationOptionRequest request) {
        this.noticeEnabled = request.getNoticeEnabled();
        this.inviteEnabled = request.getInviteEnabled();
        this.applicationEnabled = request.getApplicationEnabled();
        this.calendarEnabled = request.getCalendarEnabled();
        this.chatEnabled = request.getChatEnabled();
    }

    public boolean isEnabled(NotificationType type) {
        return switch (type) {
            case NOTICE -> noticeEnabled;
            case INVITE -> inviteEnabled;
            case APPLICATION -> applicationEnabled;
            case CALENDAR -> calendarEnabled;
            case CHAT -> chatEnabled;
        };
    }
}
