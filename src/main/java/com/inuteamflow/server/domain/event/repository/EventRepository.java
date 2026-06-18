package com.inuteamflow.server.domain.event.repository;

import com.inuteamflow.server.domain.event.entity.Event;
import com.inuteamflow.server.domain.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    // 참여 중인 이벤트 — 여러 팀에 걸칠 수 있으므로 LEFT JOIN FETCH
    @Query("""
            SELECT e FROM Event e LEFT JOIN FETCH e.team
            WHERE e.eventId IN :eventIds AND e.isSingle = :isSingle
            AND e.startAt < :endAt AND e.endAt > :startAt
            """)
    List<Event> findByEventIdInAndIsSingleAndStartAtBeforeAndEndAtAfter(
            @Param("eventIds") Collection<Long> eventIds,
            @Param("isSingle") Boolean isSingle,
            @Param("endAt") LocalDateTime endAt,
            @Param("startAt") LocalDateTime startAt
    );

    @Query("""
            SELECT e FROM Event e LEFT JOIN FETCH e.team
            WHERE e.eventId IN :eventIds AND e.isSingle = :isSingle
            AND e.startAt < :endAt
            """)
    List<Event> findByEventIdInAndIsSingleAndStartAtBefore(
            @Param("eventIds") Collection<Long> eventIds,
            @Param("isSingle") Boolean isSingle,
            @Param("endAt") LocalDateTime endAt
    );

    // 개인 이벤트 — team이 항상 null이므로 JOIN FETCH 불필요
    List<Event> findByCreatedByAndTeamIsNullAndIsSingleAndStartAtBeforeAndEndAtAfter(
            Long createdBy,
            Boolean isSingle,
            LocalDateTime endAt,
            LocalDateTime startAt
    );

    List<Event> findByCreatedByAndTeamIsNullAndIsSingleAndStartAtBefore(
            Long createdBy,
            Boolean isSingle,
            LocalDateTime endAt
    );

    // 팀 이벤트 — 같은 팀의 이벤트만 반환되므로 JOIN FETCH로 team 한 번에 로딩
    @Query("""
            SELECT e FROM Event e JOIN FETCH e.team
            WHERE e.team = :team AND e.isSingle = :isSingle
            AND e.startAt < :endAt AND e.endAt > :startAt
            """)
    List<Event> findByTeamAndIsSingleAndStartAtBeforeAndEndAtAfter(
            @Param("team") Team team,
            @Param("isSingle") Boolean isSingle,
            @Param("endAt") LocalDateTime endAt,
            @Param("startAt") LocalDateTime startAt
    );

    @Query("""
            SELECT e FROM Event e JOIN FETCH e.team
            WHERE e.team = :team AND e.isSingle = :isSingle
            AND e.startAt < :endAt
            """)
    List<Event> findByTeamAndIsSingleAndStartAtBefore(
            @Param("team") Team team,
            @Param("isSingle") Boolean isSingle,
            @Param("endAt") LocalDateTime endAt
    );
}
