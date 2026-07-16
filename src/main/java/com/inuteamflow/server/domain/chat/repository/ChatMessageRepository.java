package com.inuteamflow.server.domain.chat.repository;

import com.inuteamflow.server.domain.chat.entity.ChatMessage;
import com.inuteamflow.server.domain.chat.entity.ChatRoom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 히스토리 조회 - cursor 있음 (과거로 스크롤)
    Slice<ChatMessage> findByChatRoomAndChatMessageIdLessThanOrderByChatMessageIdDesc(
            ChatRoom chatRoom, Long cursor, Pageable pageable);

    // 히스토리 조회 - cursor 없음 (최초 호출)
    Slice<ChatMessage> findByChatRoomOrderByChatMessageIdDesc(ChatRoom chatRoom, Pageable pageable);

    // 최초 진입(anchor) - 안읽은 메시지 전체 (경계 이후, 오름차순)
    List<ChatMessage> findByChatRoomAndChatMessageIdGreaterThanOrderByChatMessageIdAsc(
            ChatRoom chatRoom, Long lastReadMessageId);

    // 최초 진입(anchor) - 경계 이하 최근 5개
    List<ChatMessage> findTop5ByChatRoomAndChatMessageIdLessThanEqualOrderByChatMessageIdDesc(
            ChatRoom chatRoom, Long lastReadMessageId);

    // 안읽음 개수 계산
    long countByChatRoomAndChatMessageIdGreaterThan(ChatRoom chatRoom, Long lastReadMessageId);

    // 채팅방 목록에서 마지막 메시지 미리보기 (N+1 방지: 여러 방의 마지막 메시지를 한 번에 조회)
    @Query(value = """
            SELECT DISTINCT ON (chat_room_id) *
            FROM chat_message
            WHERE chat_room_id IN :chatRoomIds
            ORDER BY chat_room_id, chat_message_id DESC
            """, nativeQuery = true)
    List<ChatMessage> findLatestByChatRoomIn(@Param("chatRoomIds") List<Long> chatRoomIds);

    void deleteByChatRoom(ChatRoom chatRoom);
}
