package com.inuteamflow.server.domain.event.repository;

import com.inuteamflow.server.domain.event.entity.Event;
import com.inuteamflow.server.domain.event.entity.RecurrenceException;
import com.inuteamflow.server.domain.event.entity.RecurrenceExceptionParticipant;
import com.inuteamflow.server.domain.team.entity.TeamMember;
import com.inuteamflow.server.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface RecurrenceExceptionParticipantRepository extends JpaRepository<RecurrenceExceptionParticipant, Long> {

    @Query("""
            SELECT tm.user FROM RecurrenceExceptionParticipant rep
            JOIN rep.teamMember tm
            WHERE rep.recurrenceException = :recurrenceException AND tm.user.userId != :excludeUserId
            """)
    List<User> findUsersByExceptionExcluding(
            @Param("recurrenceException") RecurrenceException recurrenceException,
            @Param("excludeUserId") Long excludeUserId
    );

    @Query("""
            SELECT rep.recurrenceException.recurrenceExceptionId
            FROM RecurrenceExceptionParticipant rep
            WHERE rep.recurrenceException.event IN :events
            AND rep.teamMember.user = :user
            """)
    Set<Long> findExceptionIdsByEventsAndUser(
            @Param("events") List<Event> events,
            @Param("user") User user
    );

    // THIS_INSTANCE sync 시 기존 참착자 교체를 위한 삭제
    void deleteByRecurrenceException(
            RecurrenceException recurrenceException
    );

    // ALL_SERIES 삭제/수정 시 — exception 삭제 전 참석자 일괄 삭제
    // 벌크 삭제는 DB에 반영되지만 영속성 컨텍스트에는 그대로 남음. 이걸 자동으로 비우는 설정
    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM RecurrenceExceptionParticipant p
            WHERE p.recurrenceException.event = :event
            """)
    void deleteByEvent(
            @Param("event") Event event
    );

    // THIS_AND_FOLLOWING 시 — 분기점 이후 exception 참석자 일괄 삭제
    @Modifying
    @Query("""
            DELETE FROM RecurrenceExceptionParticipant p
            WHERE p.recurrenceException.event = :event
            AND p.recurrenceException.originalOccurrenceAt >= :occurrenceAt
            """)
    void deleteByEventAndOriginalOccurrenceAtGreaterThanEqual(
            @Param("event") Event event,
            @Param("occurrenceAt") LocalDateTime occurrenceAt
    );

    // THIS_INSTANCE 삭제(cancel) 시 — 해당 occurrence 하나에 대한 참석자만 정리
    @Modifying
    @Query("""
            DELETE FROM RecurrenceExceptionParticipant p
            WHERE p.recurrenceException.event = :event
            AND p.recurrenceException.originalOccurrenceAt = :occurrenceAt
            """)
    void deleteByEventAndOccurrenceAt(
            @Param("event") Event event,
            @Param("occurrenceAt") LocalDateTime occurrenceAt
    );

    // 회원 탈퇴 시 — 해당 유저가 참석자로 있는 모든 exception 참석자 레코드 삭제
    @Modifying
    @Query("""
            DELETE FROM RecurrenceExceptionParticipant p 
            WHERE p.teamMember.user = :user
            """)
    void deleteByTeamMemberUser(
            @Param("user") User user
    );

    // 팀 탈퇴/방출 시 - 해당 팀원 1명이 참석자로 있는 exception 참석자 레코드만 삭제
    @Modifying
    @Query("DELETE FROM RecurrenceExceptionParticipant p WHERE p.teamMember = :teamMember")
    void deleteByTeamMember(@Param("teamMember") TeamMember teamMember);
}
