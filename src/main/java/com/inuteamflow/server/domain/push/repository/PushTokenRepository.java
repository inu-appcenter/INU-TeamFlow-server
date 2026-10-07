package com.inuteamflow.server.domain.push.repository;

import com.inuteamflow.server.domain.push.entity.PushToken;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    Optional<PushToken> findByCreatedByAndToken(Long createdBy, String token);

    void deleteByCreatedBy(Long userId);

    @Query("SELECT f.token FROM PushToken f WHERE f.createdBy = :createdBy")
    List<String> findTokenByCreatedBy(@Param("createdBy") Long createdBy);

    @Query("SELECT f.token FROM PushToken f WHERE f.createdBy IN :userIds")
    List<String> findTokenByCreatedByIn(@Param("userIds") Collection<Long> userIds);

    void deleteByTokenIn(Collection<String> tokens);
}
