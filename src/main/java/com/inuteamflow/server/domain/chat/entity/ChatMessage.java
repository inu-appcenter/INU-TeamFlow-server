package com.inuteamflow.server.domain.chat.entity;

import com.inuteamflow.server.domain.chat.enums.ChatMessageType;
import com.inuteamflow.server.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "chat_message")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_message_id")
    private Long chatMessageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private ChatMessageType messageType;

    @Column(columnDefinition = "TEXT") // 길이 제한 없앰
    private String content;

    @Column(name = "image_key")
    private String imageKey;

    @Builder
    private ChatMessage(ChatRoom chatRoom, ChatMessageType messageType, String content, String imageKey) {
        this.chatRoom = chatRoom;
        this.messageType = messageType;
        this.content = content;
        this.imageKey = imageKey;
    }

    public static ChatMessage createText(ChatRoom chatRoom, String content) {
        return ChatMessage.builder()
                .chatRoom(chatRoom)
                .messageType(ChatMessageType.TEXT)
                .content(content)
                .build();
    }

    public static ChatMessage createImage(ChatRoom chatRoom, String imageKey) {
        return ChatMessage.builder()
                .chatRoom(chatRoom)
                .messageType(ChatMessageType.IMAGE)
                .imageKey(imageKey)
                .build();
    }

    public static ChatMessage createSystem(ChatRoom chatRoom, String content) {
        return ChatMessage.builder()
                .chatRoom(chatRoom)
                .messageType(ChatMessageType.SYSTEM)
                .content(content)
                .build();
    }
}
