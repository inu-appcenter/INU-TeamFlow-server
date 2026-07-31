package com.inuteamflow.server.domain.event.repository;

import com.inuteamflow.server.domain.event.entity.Event;
import com.inuteamflow.server.domain.event.entity.RecurrenceRule;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecurrenceRuleRepository extends JpaRepository<RecurrenceRule, Long> {

    List<RecurrenceRule> findByEventIn(Collection<Event> events);

    Optional<RecurrenceRule> findByEvent(Event event);

    void deleteByEvent(Event event);

    @Modifying
    @Query(value = """
    DELETE FROM recurrence_rule_by_day
    WHERE recurrence_rule_id IN (
        SELECT rr.recurrence_rule_id
        FROM recurrence_rule rr
        JOIN event e ON rr.event_id = e.event_id
        WHERE e.created_by = :userId AND e.team_id IS NULL
    )
    """, nativeQuery = true)
    void deleteByDaysByEventCreatedByAndTeamIsNull(@Param("userId") Long userId);

    @Modifying
    @Query(value = """
    DELETE FROM recurrence_rule
    WHERE event_id IN (
        SELECT e.event_id
        FROM event e
        WHERE e.created_by = :userId
          AND e.team_id IS NULL
    )
    """, nativeQuery = true)
    void deleteByEventCreatedByAndTeamIsNull(@Param("userId") Long userId);
}
