package com.inuteamflow.server.domain.chat.controller;

import com.inuteamflow.server.domain.chat.dto.request.ChatReadRequest;
import com.inuteamflow.server.domain.chat.dto.request.ChatRoomImageUpdateRequest;
import com.inuteamflow.server.domain.chat.dto.request.DirectChatRoomCreateRequest;
import com.inuteamflow.server.domain.chat.dto.response.ChatMessageAnchorResponse;
import com.inuteamflow.server.domain.chat.dto.response.ChatMessageResponse;
import com.inuteamflow.server.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.inuteamflow.server.domain.chat.enums.ChatRoomType;
import com.inuteamflow.server.domain.user.entity.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Slice;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Chat Room Controller", description = "채팅방 컨트롤러")
public interface ChatRoomControllerDocument {

    @Operation(summary = "getMyChatRooms", description = "내 채팅방 목록 조회 (TEAM/DIRECT 타입 구분)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "채팅방 목록 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = ChatRoomSummaryResponse.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않거나 만료된 토큰",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    ResponseEntity<List<ChatRoomSummaryResponse>> getMyChatRooms(
            @RequestParam ChatRoomType type,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    );

    @Operation(summary = "getMessageInitial", description = "채팅방 최초 진입 시 메시지 조회")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "최초 진입 메시지 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ChatMessageAnchorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않거나 만료된 토큰",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "채팅방 멤버가 아님",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "채팅방을 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    ResponseEntity<ChatMessageAnchorResponse> getMessageInitial(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    );

    @Operation(summary = "getMessageHistory", description = "채팅 메시지 히스토리 조회 (커서 기반, 과거로 스크롤)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "메시지 히스토리 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ChatMessageResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않거나 만료된 토큰",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "채팅방 멤버가 아님",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "채팅방을 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    ResponseEntity<Slice<ChatMessageResponse>> getMessageHistory(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "30") int size,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    );

    @Operation(summary = "getOrCreateDirectChatRoom", description = "1:1 채팅방 진입 또는 생성 (상대방과의 방이 있으면 반환, 없으면 생성)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "1:1 채팅방 진입/생성 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ChatRoomSummaryResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "자기 자신과는 1:1 채팅 불가",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않거나 만료된 토큰",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "상대방 유저를 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    ResponseEntity<ChatRoomSummaryResponse> getOrCreateDirectChatRoom(
            @Valid @RequestBody DirectChatRoomCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    );

    @Operation(summary = "markAsRead", description = "채팅방 읽음 처리")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "읽음 처리 성공"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않거나 만료된 토큰",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "채팅방 멤버가 아님",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "채팅방을 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    ResponseEntity<Void> markAsRead(
            @PathVariable Long roomId,
            @Valid @RequestBody ChatReadRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    );

    @Operation(summary = "updateChatRoomImage", description = "팀 채팅방 이미지 설정 (팀 리더만 가능, imageKey를 null로 보내면 기본 콜라주로 리셋)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "채팅방 이미지 설정 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "TEAM 채팅방이 아님",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않거나 만료된 토큰",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "리더가 아니어서 설정 권한 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "채팅방을 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    ResponseEntity<Void> updateChatRoomImage(
            @PathVariable Long roomId,
            @Valid @RequestBody ChatRoomImageUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    );
}