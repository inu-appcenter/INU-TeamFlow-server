package com.inuteamflow.server.domain.notification.repository;

import com.inuteamflow.server.domain.notification.entity.NotificationOption;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationOptionRepository extends JpaRepository<NotificationOption, Long> {

    @Query("SELECT n FROM NotificationOption n WHERE n.user.userId = :userId")
    Optional<NotificationOption> findByUserId(@Param("userId") Long userId);

    @Query("SELECT n FROM NotificationOption n WHERE n.user.userId IN :userIds")
    List<NotificationOption> findByUserIdIn(Collection<Long> userIds);

    @Query("SELECT COUNT(n) > 0 FROM NotificationOption n WHERE n.user.userId = :userId")
    boolean existsByUserId(Long userId);
}
