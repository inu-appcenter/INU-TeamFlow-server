package com.inuteamflow.server.global.jwt.refresh;

import com.inuteamflow.server.global.enums.ClientType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    boolean existsByUserId(Long userId);

    Optional<RefreshToken> findByUserIdAndClientType(Long userId, ClientType clientType);

    Optional<RefreshToken> findByRefreshToken(String refreshToken);

    void deleteByUserId(Long userId);
}
