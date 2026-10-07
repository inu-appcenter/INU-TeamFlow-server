package com.inuteamflow.server.domain.push.repository;

import com.inuteamflow.server.domain.push.entity.PushToken;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    Optional<PushToken> findByCreatedByAndToken(Long createdBy, String token);

    void deleteByCreatedBy(Long userId);

    List<PushToken> findAllByCreatedBy(Long createdBy);

    List<PushToken> findAllByCreatedByIn(Collection<Long> userIds);

    void deleteByTokenIn(Collection<String> tokens);
}
