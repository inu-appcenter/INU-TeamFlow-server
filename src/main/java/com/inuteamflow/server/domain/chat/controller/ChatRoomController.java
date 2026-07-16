package com.inuteamflow.server.domain.chat.controller;

import com.inuteamflow.server.domain.chat.dto.request.ChatReadRequest;
import com.inuteamflow.server.domain.chat.dto.request.DirectChatRoomCreateRequest;
import com.inuteamflow.server.domain.chat.dto.response.ChatMessageAnchorResponse;
import com.inuteamflow.server.domain.chat.dto.response.ChatMessageResponse;
import com.inuteamflow.server.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.inuteamflow.server.domain.chat.enums.ChatRoomType;
import com.inuteamflow.server.domain.chat.service.ChatRoomService;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat-rooms")
public class ChatRoomController implements ChatRoomControllerDocument{

    private final ChatRoomService chatRoomService;

    // 내 채팅방 목록 조회
    @GetMapping
    public ResponseEntity<List<ChatRoomSummaryResponse>> getMyChatRooms(
            @RequestParam ChatRoomType type,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(chatRoomService.getMyChatRooms(userDetails.getUser(), type));
    }

    // 채팅방 최초 진입 시 메시지 조회
    @GetMapping("/{roomId}/messages/initial")
    public ResponseEntity<ChatMessageAnchorResponse> getMessageInitial(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(chatRoomService.getMessageInitial(roomId, userDetails.getUser()));
    }

    // 채팅 메시지 히스토리 조회 (커서 기반, 과거로 스크롤)
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<Slice<ChatMessageResponse>> getMessageHistory(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "30") int size,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(chatRoomService.getMessageHistory(roomId, cursor, size, userDetails.getUser()));
    }

    // 1:1 채팅방 진입 또는 생성
    @PostMapping("/direct")
    public ResponseEntity<ChatRoomSummaryResponse> getOrCreateDirectChatRoom(
            @Valid @RequestBody DirectChatRoomCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(chatRoomService.getOrCreateDirectChatRoom(userDetails.getUser(), request));
    }

    // 읽음 처리
    @PostMapping("/{roomId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long roomId,
            @Valid @RequestBody ChatReadRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        chatRoomService.markAsRead(roomId, userDetails.getUser(), request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
