package com.inuteamflow.server.domain.chat.repository;

import com.inuteamflow.server.domain.chat.entity.ChatMessage;
import com.inuteamflow.server.domain.chat.entity.ChatRoom;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 히스토리 조회 - cursor 있음 (과거로 스크롤), 합류 시점 이전은 차단
    Slice<ChatMessage> findByChatRoomAndChatMessageIdLessThanOrderByChatMessageIdDesc(
            ChatRoom chatRoom, Long cursor, Long visibleFromMessageId, Pageable pageable);

    // 히스토리 조회 - cursor 없음 (최초 호출), 합류 시점 이전은 차단
    Slice<ChatMessage> findByChatRoomOrderByChatMessageIdDesc(
            ChatRoom chatRoom, Long visibleFromMessageId, Pageable pageable);

    // 최초 진입(anchor) - 안읽은 메시지 전체 (경계 이후, 오름차순)
    List<ChatMessage> findByChatRoomAndChatMessageIdGreaterThanOrderByChatMessageIdAsc(
            ChatRoom chatRoom, Long lastReadMessageId);

    // 최초 진입(anchor) - 경계 이하 최근 5개, 합류 시점 이전은 차단
    List<ChatMessage> findTop5ByChatRoomAndChatMessageIdLessThanEqualAndChatMessageIdGreaterThanOrderByChatMessageIdDesc(
            ChatRoom chatRoom, Long lastReadMessageId, Long visibleFromMessageId);

    // 안읽음 개수 계산
    long countByChatRoomAndChatMessageIdGreaterThan(ChatRoom chatRoom, Long lastReadMessageId);

    // 멤버 합류 시점의 기준점 계산용 - 방의 가장 최근 메시지
    Optional<ChatMessage> findTopByChatRoomOrderByChatMessageIdDesc(ChatRoom chatRoom);

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
