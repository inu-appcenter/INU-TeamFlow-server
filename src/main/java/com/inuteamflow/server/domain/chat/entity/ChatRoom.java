package com.inuteamflow.server.domain.chat.entity;

import com.inuteamflow.server.domain.chat.enums.ChatRoomType;
import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "chat_room")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long chatRoomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "chat_room_type", nullable = false)
    private ChatRoomType chatRoomType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team; // Team 타입일 때만 값 존재

    @Builder
    private ChatRoom(ChatRoomType chatRoomType, Team team) {
        this.chatRoomType = chatRoomType;
        this.team = team;
    }

    public static ChatRoom createTeamRoom(Team team) {
        return ChatRoom.builder()
                .chatRoomType(ChatRoomType.TEAM)
                .team(team)
                .build();
    }

    public static ChatRoom createDirectRoom() {
        return ChatRoom.builder()
                .chatRoomType(ChatRoomType.DIRECT)
                .build();
    }
}
