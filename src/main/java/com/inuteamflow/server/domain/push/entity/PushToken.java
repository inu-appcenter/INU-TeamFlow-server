package com.inuteamflow.server.domain.push.entity;

import com.inuteamflow.server.domain.push.dto.req.PushTokenRequest;
import com.inuteamflow.server.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "push_token")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "push_token_id")
    private Long pushTokenId;

    @Column(name = "token")
    private String token;

    @Column(name = "device_type")
    private String deviceType;

    @Builder
    private PushToken(String token, String deviceType) {
        this.token = token;
        this.deviceType = deviceType;
    }

    public static PushToken create(PushTokenRequest request) {
        return PushToken.builder()
                .token(request.getToken())
                .deviceType(request.getDeviceType())
                .build();
    }
}
