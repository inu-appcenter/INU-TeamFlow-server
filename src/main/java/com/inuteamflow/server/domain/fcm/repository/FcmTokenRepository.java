package com.inuteamflow.server.domain.fcm.repository;

import com.inuteamflow.server.domain.fcm.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByCreatedByAndFcmToken(Long createdBy, String fcmToken);

    void deleteByCreatedBy(Long userId);

    @Query("SELECT f.fcmToken FROM FcmToken f WHERE f.createdBy = :createdBy")
    List<String> findFcmTokenByCreatedBy(@Param("createdBy") Long createdBy);

    @Query("SELECT f.fcmToken FROM FcmToken f WHERE f.createdBy IN :userIds")
    List<String> findFcmTokenByCreatedByIn(@Param("userIds") Collection<Long> userIds);

    void deleteByFcmTokenIn(Collection<String> fcmTokens);
}
