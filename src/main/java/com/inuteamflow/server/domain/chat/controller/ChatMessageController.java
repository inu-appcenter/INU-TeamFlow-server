package com.inuteamflow.server.domain.chat.controller;

import com.inuteamflow.server.domain.chat.dto.request.ChatMessageSendRequest;
import com.inuteamflow.server.domain.chat.service.ChatMessageService;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    // 클라이언트가 /pub/chat-rooms/{roomId}/messages 로 SEND하면 처리
    @MessageMapping("/chat-rooms/{roomId}/messages")
    public void sendMessage(
            @DestinationVariable Long roomId,
            @Payload ChatMessageSendRequest request,
            Principal principal
    ) {
        User sender = extractUser(principal);
        chatMessageService.sendMessage(roomId, request, sender);
    }

    private User extractUser(Principal principal) {
        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) principal;
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userDetails.getUser();
    }
}
