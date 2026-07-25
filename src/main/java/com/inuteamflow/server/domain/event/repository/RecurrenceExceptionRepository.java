package com.inuteamflow.server.domain.event.repository;

import com.inuteamflow.server.domain.event.entity.Event;
import com.inuteamflow.server.domain.event.entity.RecurrenceException;
import com.inuteamflow.server.domain.event.enums.RecurrenceExceptionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RecurrenceExceptionRepository extends JpaRepository<RecurrenceException, Long> {

    List<RecurrenceException> findByEventIn(
            Collection<Event> events
    );

    Optional<RecurrenceException> findByEventAndOriginalOccurrenceAt(
            Event event,
            LocalDateTime originalOccurrenceAt
    );

    @Query("""
    SELECT re
    FROM RecurrenceException re
    JOIN FETCH re.event e
    LEFT JOIN FETCH e.team
    WHERE re.exceptionType = :exceptionType
    AND re.event IN :events
    AND re.modifiedStartAt < :endAt
    AND re.modifiedEndAt > :startAt
    """)
    List<RecurrenceException> findModifiedOverlapping(
            @Param("exceptionType") RecurrenceExceptionType exceptionType,
            @Param("events") Collection<Event> events,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    void deleteByEvent(
            Event event
    );

    void deleteByEventAndOriginalOccurrenceAtGreaterThanEqual(
            Event event,
            LocalDateTime originalOccurrenceAt
    );

    @Modifying
    @Query("DELETE FROM RecurrenceException re WHERE re.event.createdBy = :userId AND re.event.team IS NULL")
    void deleteByEventCreatedByAndTeamIsNull(@Param("userId") Long userId);
}
