package com.inuteamflow.server.domain.notification.repository;

import com.inuteamflow.server.domain.notification.entity.NotificationOption;
import com.inuteamflow.server.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationOptionRepository extends JpaRepository<NotificationOption, Long> {
    Optional<NotificationOption> findByUser(User user);

    boolean existsByUser(User user);
}
