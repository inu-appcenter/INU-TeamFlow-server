package com.inuteamflow.server.domain.event.repository;

import com.inuteamflow.server.domain.event.entity.Event;
import com.inuteamflow.server.domain.event.entity.RecurrenceRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RecurrenceRuleRepository extends JpaRepository<RecurrenceRule, Long> {

    List<RecurrenceRule> findByEventIn(
            Collection<Event> events
    );

    Optional<RecurrenceRule> findByEvent(
            Event event
    );

    void deleteByEvent(
            Event event
    );
}
