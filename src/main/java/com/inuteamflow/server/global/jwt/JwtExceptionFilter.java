package com.inuteamflow.server.global.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.ErrorCode;
import com.inuteamflow.server.global.exception.error.ErrorResponse;
import com.inuteamflow.server.global.exception.error.RestApiException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class JwtExceptionFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    public JwtExceptionFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        response.setCharacterEncoding("UTF-8");

        try {
            filterChain.doFilter(request, response);
        } catch (RestApiException e) {
            ErrorCode errorCode = e.getErrorCode();
            log.warn("[인증 오류] 토큰 검증 실패 - code={}, 사유: {}", errorCode.getCode(), errorCode.getMessage());
            setErrorResponse(response, (CustomErrorCode) errorCode);
        }
    }

    private void setErrorResponse(HttpServletResponse response, CustomErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = ErrorResponse.create(errorCode.getCode(), errorCode.getMessage());
        String json = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(json);
    }
}
