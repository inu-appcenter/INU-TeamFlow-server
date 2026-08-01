package com.inuteamflow.server.domain.chat.dto.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomImageUpdateRequest {

    private String imageKey; // null 이면 기본 콜라주로 리셋
}
