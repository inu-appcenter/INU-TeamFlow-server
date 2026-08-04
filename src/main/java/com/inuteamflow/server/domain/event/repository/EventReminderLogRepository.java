package com.inuteamflow.server.domain.event.repository;

import com.inuteamflow.server.domain.event.entity.EventReminderLog;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventReminderLogRepository extends JpaRepository<EventReminderLog, Long> {

    List<EventReminderLog> findByEventIdIn(Collection<Long> eventIds);
}
