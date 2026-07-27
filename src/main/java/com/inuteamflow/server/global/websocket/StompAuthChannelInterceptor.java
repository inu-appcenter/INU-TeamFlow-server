package com.inuteamflow.server.global.websocket;

import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import com.inuteamflow.server.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = resolveToken(accessor.getFirstNativeHeader("Authorization"));

            if (token == null || !jwtTokenProvider.validateToken(token)) {
                throw new RestApiException(CustomErrorCode.JWT_INVALID);
            }

            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            accessor.setUser(authentication); // 이제 원본 message에 실제로 반영됨
        }

        // CONNECT 이후의 모든 프레임(SEND 등)에서도, 세션에 저장된 Principal을
        // 이 스레드의 SecurityContext에 실어줘야 JPA Auditing(@CreatedBy) 등이 동작함
        if (accessor.getUser() instanceof Authentication authentication) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        return message;
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        // 스레드 풀 재사용 시 SecurityContext가 다음 요청으로 새는 것 방지
        SecurityContextHolder.clearContext();
    }

    private String resolveToken(String bearerToken) {
        return StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")
                ? bearerToken.substring(7)
                : null;
    }
}