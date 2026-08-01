package com.inuteamflow.server.domain.notification.repository;

import com.inuteamflow.server.domain.notification.entity.Notification;
import com.inuteamflow.server.domain.notification.enums.NotificationType;
import com.inuteamflow.server.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Slice<Notification> findByReceiver(User receiver, Pageable pageable);

    Slice<Notification> findByReceiverAndType(User receiver, NotificationType type, Pageable pageable);

    Integer countByReceiverAndIsReadFalse(User receiver);

    @Query("""
            SELECT n
            FROM Notification n
            JOIN FETCH n.receiver
            WHERE n.notificationId = :id
            """)
    Optional<Notification> findByIdWithReceiver(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Notification n
            SET n.isRead = true
            WHERE n.notificationId IN :ids AND n.receiver = :receiver
            """)
    void bulkRead(@Param("ids") List<Long> ids, @Param("receiver") User receiver);

    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM Notification n
            WHERE n.notificationId IN :ids AND n.receiver = :receiver
            """)
    void bulkDelete(@Param("ids") List<Long> ids, @Param("receiver") User receiver);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.receiver = :receiver")
    void deleteByReceiver(@Param("receiver") User receiver);
}
