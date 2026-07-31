package com.inuteamflow.server.domain.chat.controller;

import com.inuteamflow.server.domain.chat.dto.request.*;
import com.inuteamflow.server.domain.chat.dto.response.ChatMessageAnchorResponse;
import com.inuteamflow.server.domain.chat.dto.response.ChatMessageResponse;
import com.inuteamflow.server.domain.chat.dto.response.ChatRoomMemberResponse;
import com.inuteamflow.server.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.inuteamflow.server.domain.chat.enums.ChatRoomType;
import com.inuteamflow.server.domain.chat.service.ChatRoomService;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat-rooms")
public class ChatRoomController implements ChatRoomControllerDocument {

    private final ChatRoomService chatRoomService;

    // 내 채팅방 목록 조회
    @GetMapping
    public ResponseEntity<List<ChatRoomSummaryResponse>> getMyChatRooms(
            @RequestParam ChatRoomType type, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.OK).body(chatRoomService.getMyChatRooms(userDetails.getUser(), type));
    }

    // 채팅방 최초 진입 시 메시지 조회
    @GetMapping("/{roomId}/messages/initial")
    public ResponseEntity<ChatMessageAnchorResponse> getMessageInitial(
            @PathVariable Long roomId, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(chatRoomService.getMessageInitial(roomId, userDetails.getUser()));
    }

    // 채팅 메시지 히스토리 조회 (커서 기반, 과거로 스크롤)
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<Slice<ChatMessageResponse>> getMessageHistory(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "30") int size,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(chatRoomService.getMessageHistory(roomId, cursor, size, userDetails.getUser()));
    }

    // 1:1 채팅방 진입 또는 생성
    @PostMapping("/direct")
    public ResponseEntity<ChatRoomSummaryResponse> getOrCreateDirectChatRoom(
            @Valid @RequestBody DirectChatRoomCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(chatRoomService.getOrCreateDirectChatRoom(userDetails.getUser(), request));
    }

    // 읽음 처리
    @PostMapping("/{roomId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long roomId,
            @Valid @RequestBody ChatReadRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        chatRoomService.markAsRead(roomId, userDetails.getUser(), request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // 팀 채팅방 이미지 설정 (리더만 가능)
    @PatchMapping("/{roomId}/image")
    public ResponseEntity<Void> updateChatRoomImage(
            @PathVariable Long roomId,
            @Valid @RequestBody ChatRoomImageUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        chatRoomService.updateTeamChatRoomImage(userDetails.getUser(), roomId, request.getImageKey());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // 팀 안에서 멤버 선택해 그룹 채팅방 생성
    @PostMapping("/group")
    public ResponseEntity<ChatRoomSummaryResponse> createGroupChatRoom(
            @Valid @RequestBody GroupChatRoomCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatRoomService.createGroupChatRoom(userDetails.getUser(), request));
    }

    // 채팅방 현재 멤버 목록 조회
    @GetMapping("/{roomId}/members")
    public ResponseEntity<List<ChatRoomMemberResponse>> getChatRoomMembers(
            @PathVariable Long roomId, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(chatRoomService.getChatRoomMembers(userDetails.getUser(), roomId));
    }

    // 채팅방에 멤버 초대
    @PostMapping("/{roomId}/invite")
    public ResponseEntity<Void> inviteToGroupChatRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody ChatRoomInviteRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        chatRoomService.inviteToGroupChatRoom(userDetails.getUser(), roomId, request);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // 채팅방 퇴장
    @DeleteMapping("/{roomId}/members/me")
    public ResponseEntity<Void> leaveGroupChatRoom(
            @PathVariable Long roomId, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        chatRoomService.leaveGroupChatRoom(userDetails.getUser(), roomId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
