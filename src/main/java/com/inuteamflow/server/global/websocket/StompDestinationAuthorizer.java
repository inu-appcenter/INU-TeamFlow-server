package com.inuteamflow.server.global.websocket;

import com.inuteamflow.server.domain.chat.repository.ChatRoomMemberRepository;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * STOMP 목적지 단위 인가를 담당한다.
 *
 * <p>SimpleBroker는 구독 경로의 와일드카드({@code /sub/**})를 허용하므로, 금지 경로를 나열하지 않고
 * 허용한 형태의 경로만 통과시킨다. 새 구독 경로를 추가하면 이곳에도 등록해야 한다.</p>
 */
@Component
@RequiredArgsConstructor
public class StompDestinationAuthorizer {

    private static final Pattern CHAT_ROOM_TOPIC = Pattern.compile("/sub/chat-rooms/(\\d{1,18})(?:/read)?");
    private static final Pattern USER_CHAT_ROOMS_TOPIC = Pattern.compile("/sub/users/(\\d{1,18})/chat-rooms");
    private static final String PUBLISH_PREFIX = "/pub/";

    private final ChatRoomMemberRepository chatRoomMemberRepository;

    /**
     * 구독 요청을 검증한다. 채팅방 topic은 해당 방의 멤버만, 개인 topic은 본인만 구독할 수 있다.
     *
     * @param principal CONNECT에서 인증된 사용자
     * @param destination 구독하려는 경로
     * @throws RestApiException 인증되지 않았거나 구독 권한이 없는 경우
     */
    public void authorizeSubscribe(Principal principal, String destination) {
        Long userId = extractUserId(principal);

        if (destination != null) {
            Matcher roomTopic = CHAT_ROOM_TOPIC.matcher(destination);
            if (roomTopic.matches()
                    && chatRoomMemberRepository.existsByChatRoom_ChatRoomIdAndUser_UserId(
                            Long.parseLong(roomTopic.group(1)), userId)) {
                return;
            }

            Matcher userTopic = USER_CHAT_ROOMS_TOPIC.matcher(destination);
            if (userTopic.matches() && userId.equals(Long.parseLong(userTopic.group(1)))) {
                return;
            }
        }

        throw new RestApiException(CustomErrorCode.CHAT_ROOM_FORBIDDEN);
    }

    /**
     * 발행 요청을 검증한다. 브로커 경로로 직접 보내면 서비스의 검증을 건너뛰므로 {@code /pub/} 경로만 허용한다.
     *
     * @param principal CONNECT에서 인증된 사용자
     * @param destination 발행하려는 경로
     * @throws RestApiException 인증되지 않았거나 허용되지 않은 경로인 경우
     */
    public void authorizeSend(Principal principal, String destination) {
        extractUserId(principal);

        if (destination == null || !destination.startsWith(PUBLISH_PREFIX)) {
            throw new RestApiException(CustomErrorCode.CHAT_ROOM_FORBIDDEN);
        }
    }

    private Long extractUserId(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return userDetails.getUser().getUserId();
        }
        throw new RestApiException(CustomErrorCode.JWT_INVALID);
    }
}
