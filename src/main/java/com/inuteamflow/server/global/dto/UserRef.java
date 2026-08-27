package com.inuteamflow.server.global.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "사용자 참조 DTO")
public class UserRef {

    @Schema(description = "사용자 ID", example = "11")
    private Long userId;

    @Schema(description = "사용자 이름", example = "김OO")
    private String name;

    public static UserRef of(Long userId, String name) {
        return new UserRef(userId, name);
    }
}
