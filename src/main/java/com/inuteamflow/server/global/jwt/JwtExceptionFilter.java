package com.inuteamflow.server.global.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.ErrorResponse;
import com.inuteamflow.server.global.exception.error.RestApiException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtExceptionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtExceptionFilter.class);
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
            log.error("JwtExceptionFilter: JWT Exception occurred - {}", e.getMessage());
            setErrorResponse(response, (CustomErrorCode) e.getErrorCode());
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
