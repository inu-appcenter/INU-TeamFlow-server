package com.inuteamflow.server.global.exception.handler;

import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.ErrorCode;
import com.inuteamflow.server.global.exception.error.ErrorResponse;
import com.inuteamflow.server.global.exception.error.RestApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RestApiException.class)
    public ResponseEntity<ErrorResponse> handleRestApiException(RestApiException e) {
        ErrorCode errorCode = e.getErrorCode();
        if (errorCode.getHttpStatus().is5xxServerError()) {
            log.error(
                    "RestApiException: code={}, status={}",
                    errorCode.getCode(),
                    errorCode.getHttpStatus().value(),
                    e);
        } else {
            log.warn(
                    "RestApiException: code={}, status={}, message={}",
                    errorCode.getCode(),
                    errorCode.getHttpStatus().value(),
                    errorCode.getMessage());
        }
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.create(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        ErrorCode errorCode = CustomErrorCode.COMMON_INVALID_REQUEST;
        log.warn(
                "MethodArgumentNotValidException: code={}, status={}, message={}",
                errorCode.getCode(),
                errorCode.getHttpStatus().value(),
                errorCode.getMessage());

        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : errorCode.getMessage();

        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.create(errorCode.getCode(), message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e) {
        ErrorCode errorCode = CustomErrorCode.COMMON_INVALID_REQUEST_TYPE;
        log.warn(
                "MethodArgumentTypeMismatchException: code={}, status={}, message={}",
                errorCode.getCode(),
                errorCode.getHttpStatus().value(),
                errorCode.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.create(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
            MissingServletRequestParameterException e) {
        ErrorCode errorCode = CustomErrorCode.COMMON_INVALID_PARAMETER;
        log.warn(
                "MissingServletRequestParameterException: code={}, status={}, message={}",
                errorCode.getCode(),
                errorCode.getHttpStatus().value(),
                errorCode.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.create(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException e) {
        ErrorCode errorCode = CustomErrorCode.COMMON_HANDLER_NOT_FOUND;
        log.warn(
                "NoHandlerFoundException: code={}, status={}, message={}",
                errorCode.getCode(),
                errorCode.getHttpStatus().value(),
                errorCode.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ErrorResponse.create(errorCode.getCode(), ""));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException e) {
        ErrorCode errorCode = CustomErrorCode.AUTH_LOGIN_FAILED;
        log.warn(
                "BadCredentialsException: code={}, status={}, message={}",
                errorCode.getCode(),
                errorCode.getHttpStatus().value(),
                errorCode.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.create(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLockedException(LockedException e) {
        ErrorCode errorCode = CustomErrorCode.USER_BANNED;
        log.warn(
                "LockedException: code={}, status={}, message={}",
                errorCode.getCode(),
                errorCode.getHttpStatus().value(),
                errorCode.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.create(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception e) {
        ErrorCode errorCode = CustomErrorCode.COMMON_INTERNAL_SERVER_ERROR;
        log.error(
                "UnexpectedException: code={}, status={}",
                errorCode.getCode(),
                errorCode.getHttpStatus().value(),
                e);
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.create(errorCode.getCode(), errorCode.getMessage()));
    }
}
