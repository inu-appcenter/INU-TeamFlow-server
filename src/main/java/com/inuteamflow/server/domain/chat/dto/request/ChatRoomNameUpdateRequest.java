package com.inuteamflow.server.domain.chat.dto.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomNameUpdateRequest {

    private String roomName; // null 이면 공유 기본 이름으로 리셋

}
