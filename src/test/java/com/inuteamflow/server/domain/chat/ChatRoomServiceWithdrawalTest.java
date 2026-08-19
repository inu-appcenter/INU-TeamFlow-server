package com.inuteamflow.server.domain.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.inuteamflow.server.domain.chat.dto.response.ChatMessageResponse;
import com.inuteamflow.server.domain.chat.entity.ChatMessage;
import com.inuteamflow.server.domain.chat.entity.ChatRoom;
import com.inuteamflow.server.domain.chat.entity.ChatRoomMember;
import com.inuteamflow.server.domain.chat.repository.ChatMessageRepository;
import com.inuteamflow.server.domain.chat.repository.ChatRoomMemberRepository;
import com.inuteamflow.server.domain.chat.repository.ChatRoomRepository;
import com.inuteamflow.server.domain.chat.service.ChatRoomService;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.domain.user.enums.Department;
import com.inuteamflow.server.domain.user.enums.Role;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.domain.user.service.UserService;
import com.inuteamflow.server.global.s3.S3Service;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Slice;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link UserService}의 회원 탈퇴 이후 {@link ChatRoomService}가 보존된 채팅 메시지를 조회하는 흐름을 실제 Repository와 H2
 * 데이터베이스로 검증한다.
 * - 발신자 계정과 채팅방 멤버십은 삭제되지만 기존 메시지는 유지되는지 확인한다.
 * - 삭제된 발신자의 메시지를 예외 없이 placeholder 작성자 정보로 반환하는지 확인한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ChatRoomServiceWithdrawalTest {

    @Autowired
    private UserService userService;

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private S3Service s3Service;

    private User member1;
    private User member2;
    private ChatRoom chatRoom;
    private ChatMessage message;

    @BeforeEach
    void setUp() {
        member1 = createUser("member1", "member1@inu.ac.kr");
        member2 = createUser("member2", "member2@inu.ac.kr");

        actingAs(member1);
        chatRoom = chatRoomRepository.save(ChatRoom.createDirectRoom());
        chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom, member1));
        chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom, member2));

        message = ChatMessage.createText(chatRoom, "member1이 보낸 메시지");
        message.assignAuditor(member1.getUserId());
        chatMessageRepository.save(message);

        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("발신자가 회원 탈퇴해도 남은 메시지를 탈퇴 사용자 placeholder로 조회한다")
    void getMessageHistory_returnsWithdrawnSenderPlaceholder_whenSenderDeleted() {
        // given: setUp 에서 구성한 발신자/채팅방/메시지의 식별자를 확보
        Long member1Id = member1.getUserId();
        Long member2Id = member2.getUserId();
        Long chatRoomId = chatRoom.getChatRoomId();
        Long messageId = message.getChatMessageId();

        // when: 발신자가 회원 탈퇴해 사용자와 채팅방 멤버십이 삭제됨
        userService.deleteUser(userRepository.getReferenceById(member1Id));
        entityManager.flush();
        entityManager.clear();

        // then: 메시지와 남은 사용자의 멤버십은 유지되지만 발신자 조회 결과는 존재하지 않음
        assertThat(userRepository.findById(member1Id)).isEmpty();
        assertThat(chatMessageRepository.findById(messageId)).isPresent();

        User reloadedRemainingMember = userRepository.findById(member2Id).orElseThrow();
        assertThat(chatRoomMemberRepository.findByChatRoomAndUser(
                        chatRoomRepository.findById(chatRoomId).orElseThrow(), reloadedRemainingMember))
                .isPresent();

        // 삭제된 발신자를 조회하지 못하더라도 메시지 기록은 placeholder 작성자 정보와 함께 반환되어야 함
        Slice<ChatMessageResponse> result =
                chatRoomService.getMessageHistory(chatRoomId, null, 20, reloadedRemainingMember);

        assertThat(result.getContent()).singleElement().satisfies(response -> {
            assertThat(response.getContent()).isEqualTo("member1이 보낸 메시지");
            assertThat(response.getSenderName()).isEqualTo("(탈퇴한 사용자)");
        });
    }

    private User createUser(String username, String email) {
        return userRepository.save(User.builder()
                .username(username)
                .email(email)
                .password("encoded-password")
                .name(username)
                .department(Department.COMPUTER_SCIENCE)
                .studentNumber(null)
                .isSchoolVerified(false)
                .role(Role.USER)
                .imageKey(null)
                .build());
    }

    private void actingAs(User user) {
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }
}
