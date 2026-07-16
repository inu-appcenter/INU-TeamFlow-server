package com.inuteamflow.server.domain.chat.repository;

import com.inuteamflow.server.domain.chat.entity.ChatRoom;
import com.inuteamflow.server.domain.chat.entity.ChatRoomMember;
import com.inuteamflow.server.domain.chat.enums.ChatRoomType;
import com.inuteamflow.server.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    // 특정 방에서 이 유저의 멤버 정보 조회 (lastReadMessageId 확인, 권한 체크용)
    Optional<ChatRoomMember> findByChatRoomAndUser(ChatRoom chatRoom, User user);

    // 방 접근 권한 체크용 (이 유저가 이 방 멤버인지)
    boolean existsByChatRoomAndUser(ChatRoom chatRoom, User user);

    // 팀인지 1:1 인지 분기해서 조회
    @Query("SELECT crm FROM ChatRoomMember crm JOIN FETCH crm.chatRoom WHERE crm.user = :user AND crm.chatRoom.chatRoomType = :type")
    List<ChatRoomMember> findByUserAndChatRoomTypeWithChatRoom(@Param("user") User user, @Param("type") ChatRoomType type);

    // 팀 삭제 시 채팅방도 삭제
    @Modifying
    @Query("DELETE FROM ChatMessage cm WHERE cm.chatRoom = :chatRoom")
    void deleteByChatRoom(@Param("chatRoom") ChatRoom chatRoom);

    // DIRECT 방 목록의 "나를 제외한 상대방"을 배치로 한 번에 조회 (N+1 방지)
    @Query("""
        SELECT crm FROM ChatRoomMember crm
        JOIN FETCH crm.user
        WHERE crm.chatRoom.chatRoomId IN :chatRoomIds
        AND crm.user <> :me
        """)
    List<ChatRoomMember> findPartnersByChatRoomIdIn(@Param("chatRoomIds") List<Long> chatRoomIds, @Param("me") User me);

    // 두 유저가 함께 속한 DIRECT 방이 있는지 조회
    @Query("""
        SELECT crm1.chatRoom FROM ChatRoomMember crm1
        JOIN ChatRoomMember crm2 ON crm1.chatRoom = crm2.chatRoom
        WHERE crm1.chatRoom.chatRoomType = :type
        AND crm1.user = :userA
        AND crm2.user = :userB
        """)
    Optional<ChatRoom> findDirectRoomBetween(@Param("userA") User userA, @Param("userB") User userB, @Param("type") ChatRoomType type);
}
