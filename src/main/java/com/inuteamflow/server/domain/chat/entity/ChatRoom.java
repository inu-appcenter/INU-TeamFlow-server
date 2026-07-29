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
    private Team team; // TEAM/GROUP 타입일 때만 값 존재

    @Column(name = "image_key")
    private String imageKey;

    @Column(name = "room_name")
    private String roomName; // GROUP 타입 커스텀 방 이름 (없으면 참여자 이름으로 자동 표시)

    @Builder
    private ChatRoom(ChatRoomType chatRoomType, Team team, String roomName) {
        this.chatRoomType = chatRoomType;
        this.team = team;
        this.roomName = roomName;
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

    public static ChatRoom createGroupRoom(Team team, String roomName) {
        return ChatRoom.builder()
                .chatRoomType(ChatRoomType.GROUP)
                .team(team)
                .roomName(roomName)
                .build();
    }

    public void updateImage(String imageKey) {
        this.imageKey = imageKey; // null 넘기면 기본 콜라주로 리셋
    }
}
