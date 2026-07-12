package com.inuteamflow.server.domain.event.repository;

import com.inuteamflow.server.domain.event.entity.Event;
import com.inuteamflow.server.domain.event.entity.EventParticipant;
import com.inuteamflow.server.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {

    List<EventParticipant> findByEvent(Event event);

    @Query("""
            SELECT ep.event.eventId 
            FROM EventParticipant ep 
            WHERE ep.teamMember.user = :user
            """)
    List<Long> findEventIdsByUser(@Param("user") User user);

    void deleteByEvent(Event event);

    @Modifying
    @Query("DELETE FROM EventParticipant ep WHERE ep.teamMember.user = :user")
    void deleteByTeamMemberUser(@Param("user") User user);
}
