package com.inuteamflow.server.global.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.inuteamflow.server.domain.chat.entity.ChatRoom;
import com.inuteamflow.server.domain.chat.entity.ChatRoomMember;
import com.inuteamflow.server.domain.chat.repository.ChatRoomMemberRepository;
import com.inuteamflow.server.domain.chat.repository.ChatRoomRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.domain.user.enums.Department;
import com.inuteamflow.server.domain.user.enums.Role;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.global.enums.ClientType;
import com.inuteamflow.server.global.jwt.JwtTokenProvider;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * STOMP 구독/발행 인가를 검증한다.
 * - 랜덤 포트로 서버를 띄우고 H2에 데이터를 넣은 뒤, 실제 WebSocket으로 STOMP 프레임을 주고받는다.
 * - 프레임 종류를 직접 바꿔 보내야 해서 STOMP 클라이언트 라이브러리 대신 원문 프레임을 사용한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class StompAuthorizationTest {

    private static final String PAYLOAD = "{\"content\":\"hello\"}";

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private SimpUserRegistry simpUserRegistry;

    private User member;
    private User outsider;
    private Long roomId;

    @BeforeEach
    void setUp() {
        member = saveUser();
        outsider = saveUser();

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(new UserDetailsImpl(member), ""));
        ChatRoom room = chatRoomRepository.save(ChatRoom.createDirectRoom());
        chatRoomMemberRepository.save(ChatRoomMember.create(room, member));
        SecurityContextHolder.clearContext();

        roomId = room.getChatRoomId();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/sub/chat-rooms/%d", "/sub/chat-rooms/%d/read"})
    @DisplayName("방 멤버는 채팅방 topic을 구독해 메시지를 받는다")
    void memberCanSubscribeRoomTopic(String format) throws Exception {
        String destination = String.format(format, roomId);
        try (Client client = connect(member)) {
            client.subscribe(destination);
            awaitSubscription(member, destination);

            messagingTemplate.convertAndSend(destination, "hello");

            assertThat(client.next()).startsWith("MESSAGE").contains("hello");
        }
    }

    @Test
    @DisplayName("본인의 채팅방 목록 topic은 구독할 수 있다")
    void userCanSubscribeOwnTopic() throws Exception {
        String destination = "/sub/users/" + member.getUserId() + "/chat-rooms";
        try (Client client = connect(member)) {
            client.subscribe(destination);
            awaitSubscription(member, destination);

            messagingTemplate.convertAndSend(destination, "hello");

            assertThat(client.next()).startsWith("MESSAGE").contains("hello");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/sub/chat-rooms/%d", "/sub/chat-rooms/%d/read"})
    @DisplayName("방 멤버가 아니면 채팅방 topic을 구독할 수 없다")
    void outsiderCannotSubscribeRoomTopic(String format) throws Exception {
        try (Client client = connect(outsider)) {
            client.subscribe(String.format(format, roomId));

            assertThat(client.next()).startsWith("ERROR");
        }
    }

    @Test
    @DisplayName("다른 사람의 채팅방 목록 topic은 구독할 수 없다")
    void userCannotSubscribeOthersTopic() throws Exception {
        try (Client client = connect(outsider)) {
            client.subscribe("/sub/users/" + member.getUserId() + "/chat-rooms");

            assertThat(client.next()).startsWith("ERROR");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/sub/**", "/sub/chat-rooms/*", "/sub/chat-rooms/{id}", "/sub/users/*/chat-rooms"})
    @DisplayName("와일드카드 구독은 거부한다")
    void wildcardSubscriptionIsRejected(String destination) throws Exception {
        try (Client client = connect(member)) {
            client.subscribe(destination);

            assertThat(client.next()).startsWith("ERROR");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"CONNECT", "STOMP"})
    @DisplayName("토큰 없는 연결은 CONNECT와 STOMP 프레임 모두 거부한다")
    void connectWithoutTokenIsRejected(String command) throws Exception {
        try (Client client = new Client()) {
            client.raw(command + "\naccept-version:1.2\nhost:localhost\n\n\0");

            assertThat(client.next()).startsWith("ERROR");
        }
    }

    @Test
    @DisplayName("연결 인증 없이 보낸 구독은 거부한다")
    void subscribeWithoutConnectIsRejected() throws Exception {
        try (Client client = new Client()) {
            client.subscribe("/sub/chat-rooms/" + roomId);

            assertThat(client.next()).startsWith("ERROR");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"SEND", "MESSAGE"})
    @DisplayName("클라이언트는 브로커 경로로 직접 발행할 수 없다")
    void clientCannotPublishToBroker(String command) throws Exception {
        String destination = "/sub/chat-rooms/" + roomId;
        try (Client receiver = connect(member);
                Client forger = connect(outsider)) {
            receiver.subscribe(destination);
            awaitSubscription(member, destination);

            forger.raw(
                    command + "\ndestination:" + destination + "\ncontent-type:application/json\n\n" + PAYLOAD + "\0");

            assertThat(forger.next()).startsWith("ERROR");
            assertThat(receiver.next()).isNull();
        }
    }

    private User saveUser() {
        String id = UUID.randomUUID().toString().substring(0, 8);
        return userRepository.save(User.builder()
                .username("stomp-" + id)
                .email(id + "@test.local")
                .password("password")
                .name("사용자" + id)
                .department(Department.MATHEMATICS)
                .isSchoolVerified(true)
                .role(Role.USER)
                .build());
    }

    private Client connect(User user) throws Exception {
        String token = jwtTokenProvider
                .generateTokenByUsername(user.getUsername(), ClientType.WEB)
                .getAccessToken();
        Client client = new Client();
        client.raw("CONNECT\naccept-version:1.2\nhost:localhost\nAuthorization:Bearer " + token + "\n\n\0");
        assertThat(client.next()).startsWith("CONNECTED");
        return client;
    }

    // SimpleBroker는 RECEIPT를 보내지 않으므로, 구독이 등록될 때까지 레지스트리를 확인한다.
    private void awaitSubscription(User user, String destination) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            if (!simpUserRegistry
                    .findSubscriptions(subscription -> subscription
                                    .getDestination()
                                    .equals(destination)
                            && subscription.getSession().getUser().getName().equals(user.getUsername()))
                    .isEmpty()) {
                Thread.sleep(100); // 레지스트리 등록과 브로커 등록은 별도로 처리됨
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("구독이 등록되지 않았습니다: " + destination);
    }

    private class Client implements WebSocket.Listener, AutoCloseable {

        private final BlockingQueue<String> frames = new LinkedBlockingQueue<>();
        private final StringBuilder buffer = new StringBuilder();
        private final HttpClient httpClient = HttpClient.newHttpClient();
        private final WebSocket socket;

        Client() throws Exception {
            socket = httpClient
                    .newWebSocketBuilder()
                    .buildAsync(URI.create("ws://localhost:" + port + "/ws-chat"), this)
                    .get(5, TimeUnit.SECONDS);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                if (!buffer.toString().isBlank()) { // 하트비트 제외
                    frames.add(buffer.toString());
                }
                buffer.setLength(0);
            }
            webSocket.request(1);
            return null;
        }

        void raw(String frame) throws Exception {
            socket.sendText(frame, true).get(5, TimeUnit.SECONDS);
        }

        void subscribe(String destination) throws Exception {
            raw("SUBSCRIBE\nid:sub-0\ndestination:" + destination + "\n\n\0");
        }

        String next() throws InterruptedException {
            return frames.poll(2, TimeUnit.SECONDS);
        }

        @Override
        public void close() {
            socket.abort();
        }
    }
}
