package com.inuteamflow.server.domain.event.repository;

import com.inuteamflow.server.domain.event.entity.Event;
import com.inuteamflow.server.domain.event.entity.RecurrenceException;
import com.inuteamflow.server.domain.event.enums.RecurrenceExceptionType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecurrenceExceptionRepository extends JpaRepository<RecurrenceException, Long> {

    List<RecurrenceException> findByEventIn(Collection<Event> events);

    Optional<RecurrenceException> findByEventAndOriginalOccurrenceAt(Event event, LocalDateTime originalOccurrenceAt);

    // 원래 회차가 조회 범위에 걸치거나, 수정된 시각이 조회 범위와 겹치는 예외만 조회한다.
    @Query("""
    SELECT re
    FROM RecurrenceException re
    WHERE re.event IN :events
    AND (
        (re.originalOccurrenceAt > :occurrenceFrom AND re.originalOccurrenceAt < :endAt)
        OR (re.exceptionType = :modifiedType AND re.modifiedStartAt < :endAt AND re.modifiedEndAt > :startAt)
    )
    """)
    List<RecurrenceException> findAffectingRange(
            @Param("events") Collection<Event> events,
            @Param("modifiedType") RecurrenceExceptionType modifiedType,
            @Param("occurrenceFrom") LocalDateTime occurrenceFrom,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt);

    void deleteByEvent(Event event);

    void deleteByEventAndOriginalOccurrenceAtGreaterThanEqual(Event event, LocalDateTime originalOccurrenceAt);

    @Modifying
    @Query("DELETE FROM RecurrenceException re WHERE re.event.createdBy = :userId AND re.event.team IS NULL")
    void deleteByEventCreatedByAndTeamIsNull(@Param("userId") Long userId);
}
