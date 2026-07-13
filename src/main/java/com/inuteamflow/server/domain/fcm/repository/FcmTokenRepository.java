package com.inuteamflow.server.domain.fcm.repository;

import com.inuteamflow.server.domain.fcm.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByCreatedByAndFcmToken(Long createdBy, String fcmToken);

    void deleteByCreatedBy(Long userId);

    List<String> findFcmTokenByCreatedBy(Long createdBy);

    void deleteByFcmTokenIn(Collection<String> fcmTokens);
}
