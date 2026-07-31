package com.inuteamflow.server.domain.chat.repository;

import com.inuteamflow.server.domain.chat.entity.ChatRoom;
import com.inuteamflow.server.domain.chat.enums.ChatRoomType;
import com.inuteamflow.server.domain.team.entity.Team;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 팀 생성 시 만들어진 팀 채팅방 조회 (팀당 1개)
    Optional<ChatRoom> findByTeamAndChatRoomType(Team team, ChatRoomType chatRoomType);

    // 팀 삭제 시 그 팀의 모든 채팅방 (TEAM+GROUP) 조회용
    List<ChatRoom> findAllByTeam(Team team);
}
