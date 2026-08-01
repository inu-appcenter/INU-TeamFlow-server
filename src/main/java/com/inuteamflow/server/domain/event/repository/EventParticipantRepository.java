package com.inuteamflow.server.domain.event.repository;

import com.inuteamflow.server.domain.event.entity.Event;
import com.inuteamflow.server.domain.event.entity.EventParticipant;
import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.domain.user.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {

    List<EventParticipant> findByEvent(Event event);

    @Query("""
    SELECT ep.event.eventId
    FROM EventParticipant ep
    WHERE ep.teamMember.user = :user
    """)
    List<Long> findEventIdsByUser(@Param("user") User user);

    @Query("""
    SELECT tm.user FROM EventParticipant ep
    JOIN ep.teamMember tm
    WHERE ep.event = :event AND tm.user.userId != :excludeUserId
    """)
    List<User> findUsersByEventExcluding(@Param("event") Event event, @Param("excludeUserId") Long excludeUserId);

    @Query("""
    SELECT ep FROM EventParticipant ep
    JOIN FETCH ep.teamMember tm
    JOIN FETCH tm.user
    WHERE ep.event IN :events
    """)
    List<EventParticipant> findByEventInWithMember(@Param("events") List<Event> events);

    void deleteByEvent(Event event);

    @Modifying
    @Query("DELETE FROM EventParticipant ep WHERE ep.teamMember.user = :user")
    void deleteByTeamMemberUser(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM EventParticipant ep WHERE ep.teamMember = :teamMember")
    void deleteByTeamMember(@Param("teamMember") TeamMember teamMember);
}
