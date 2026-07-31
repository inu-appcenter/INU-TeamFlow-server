package com.inuteamflow.server.domain.fcm.entity;

import com.inuteamflow.server.domain.fcm.dto.req.FcmRequest;
import com.inuteamflow.server.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "fcm_token")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fcm_token_id")
    private Long fcmTokenId;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Column(name = "device_type")
    private String deviceType;

    @Builder
    private FcmToken(String fcmToken, String deviceType) {
        this.fcmToken = fcmToken;
        this.deviceType = deviceType;
    }

    public static FcmToken create(FcmRequest request) {
        return FcmToken.builder()
                .fcmToken(request.getToken())
                .deviceType(request.getDeviceType())
                .build();
    }
}
