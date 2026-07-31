package com.inuteamflow.server.domain.notification.entity;

import com.inuteamflow.server.domain.notification.enums.NotificationType;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "notification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(name = "title")
    private String title;

    @Column(name = "content")
    private String content;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(name = "redirect_url")
    private String redirectUrl;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @Builder
    private Notification(
            User receiver, String title, String content, NotificationType type, String redirectUrl, Boolean isRead) {
        this.receiver = receiver;
        this.title = title;
        this.content = content;
        this.type = type;
        this.redirectUrl = redirectUrl;
        this.isRead = isRead;
    }

    public static Notification create(
            User receiver, String title, String content, NotificationType type, String redirectUrl) {
        return Notification.builder()
                .receiver(receiver)
                .title(title)
                .content(content)
                .type(type)
                .redirectUrl(redirectUrl)
                .isRead(false)
                .build();
    }

    public void read() {
        this.isRead = true;
    }
}
