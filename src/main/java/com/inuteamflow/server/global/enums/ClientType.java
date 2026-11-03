package com.inuteamflow.server.global.enums;

import java.time.Duration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClientType {
    WEB(Duration.ofDays(3)),
    MOBILE(Duration.ofDays(30));

    public static final String HEADER_NAME = "X-Client-Type";

    private final Duration refreshTokenValidTime;

    /**
     * {@code X-Client-Type} 헤더 값을 클라이언트 타입으로 변환한다.
     *
     * @param headerValue 헤더 값
     * @return {@code mobile}이면 {@link #MOBILE}, 헤더가 없거나 그 외 값이면 {@link #WEB}
     */
    public static ClientType from(String headerValue) {
        return MOBILE.name().equalsIgnoreCase(headerValue) ? MOBILE : WEB;
    }
}
