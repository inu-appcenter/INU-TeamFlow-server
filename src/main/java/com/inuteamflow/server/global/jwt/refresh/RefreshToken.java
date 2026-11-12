package com.inuteamflow.server.global.jwt.refresh;

import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.global.enums.ClientType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "refresh_token", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "client_type"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false)
    private ClientType clientType;

    @Column(nullable = false, unique = true, length = 500)
    private String refreshToken;

    @Builder
    public RefreshToken(User user, ClientType clientType, String refreshToken) {
        this.userId = user.getUserId();
        this.clientType = clientType != null ? clientType : ClientType.WEB;
        this.refreshToken = refreshToken;
    }

    public void updateToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
