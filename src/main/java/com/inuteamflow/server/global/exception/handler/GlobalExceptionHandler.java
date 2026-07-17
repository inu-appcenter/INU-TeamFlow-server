package com.inuteamflow.server.global.exception.handler;

import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.ErrorCode;
import com.inuteamflow.server.global.exception.error.ErrorResponse;
import com.inuteamflow.server.global.exception.error.RestApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.support.MethodArgumentTypeMismatchException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RestApiException.class)
    public ResponseEntity<ErrorResponse> handleRestApiException(RestApiException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("RestApiException", e);
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.create(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        ErrorCode errorCode = CustomErrorCode.COMMON_INVALID_REQUEST;
        log.error("MethodArgumentNotValidException", e);

        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : errorCode.getMessage();

        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.create(errorCode.getCode(), message));
    }


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        ErrorCode errorCode = CustomErrorCode.COMMON_INVALID_REQUEST_TYPE;
        log.error("MethodArgumentTypeMismatchException", e);
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.create(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(MissingServletRequestParameterException e) {
        ErrorCode errorCode = CustomErrorCode.COMMON_INVALID_PARAMETER;
        log.error("MissingServletRequestParameterException", e);
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.create(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException e) {
        ErrorCode errorCode = CustomErrorCode.COMMON_HANDLER_NOT_FOUND;
        log.error("NoHandlerFoundException", e);
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.create(errorCode.getCode(), ""));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException e) {
        ErrorCode errorCode = CustomErrorCode.AUTH_LOGIN_FAILED;
        log.error("BadCredentialsException", e);
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.create(errorCode.getCode(), errorCode.getMessage()));
    }
}
