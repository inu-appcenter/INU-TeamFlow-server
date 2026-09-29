package com.inuteamflow.server.global.exception.error;

import lombok.Getter;

@Getter
public class RestApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String customMessage;

    public RestApiException(ErrorCode errorCode) {
        this.errorCode = errorCode;
        this.customMessage = null;
    }

    public RestApiException(ErrorCode errorCode, String customMessage) {
        this.errorCode = errorCode;
        this.customMessage = customMessage;
    }

    public String getMessage() {
        return customMessage != null ? customMessage : errorCode.getMessage();
    }
}
