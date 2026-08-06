package com.inuteamflow.server.domain.chat.entity;

import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.global.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "chat_room_member", uniqueConstraints = @UniqueConstraint(columnNames = {"chat_room_id", "user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatRoomMemBerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(name = "visible_from_message_id", nullable = false)
    private Long visibleFromMessageId; // 이 값보다 큰 메시지부터 조회 가능 (중간 합류자 이전 내역 차단)

    @Column(name = "custom_room_name")
    private String customRoomName; // 이 멤버만 보는 커스텀 방 이름 (GROUP 전용)

    @Column(name = "custom_image_key")
    private String customImageKey; // 이 멤버만 보는 커스텀 방 이미지 (GROUP 전용)

    @Builder
    private ChatRoomMember(ChatRoom chatRoom, User user, Long visibleFromMessageId) {
        this.chatRoom = chatRoom;
        this.user = user;
        this.visibleFromMessageId = visibleFromMessageId;
    }

    // 방 생성 시점의 원년 멤버 - 기존 메시지가 있을 수 없으니 제한 없음
    public static ChatRoomMember create(ChatRoom chatRoom, User user) {
        return ChatRoomMember.builder()
                .chatRoom(chatRoom)
                .user(user)
                .visibleFromMessageId(0L)
                .build();
    }

    // 이미 메시지가 쌓인 기존 방에 나중에 합류하는 멤버 - 합류 시점 이전 메시지는 안 보이게 기준점을 둠
    public static ChatRoomMember createJoiningExisting(ChatRoom chatRoom, User user, Long visibleFromMessageId) {
        return ChatRoomMember.builder()
                .chatRoom(chatRoom)
                .user(user)
                .visibleFromMessageId(visibleFromMessageId != null ? visibleFromMessageId : 0L)
                .build();
    }

    public void updateLastReadMessageId(Long messageId) {
        this.lastReadMessageId = messageId;
    }

    public void updateCustomRoomName(String customRoomName) {
        this.customRoomName = customRoomName; // null 넘기면 공유 기본 이름으로 리셋
    }

    public void updateCustomImageKey(String customImageKey) {
        this.customImageKey = customImageKey; // null 넘기면 공유 기본 이미지로 리셋
    }
}
