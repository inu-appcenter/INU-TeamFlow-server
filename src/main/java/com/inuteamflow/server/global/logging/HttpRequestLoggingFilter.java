package com.inuteamflow.server.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HttpRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpRequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startedAt = System.currentTimeMillis();

        MDC.put("traceId", UUID.randomUUID().toString().replace("-", ""));
        MDC.put("requestId", UUID.randomUUID().toString());
        MDC.put("method", request.getMethod());
        MDC.put("path", request.getRequestURI());

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;

            MDC.put("event", "http_request_completed");
            MDC.put("status", String.valueOf(response.getStatus()));
            MDC.put("durationMs", String.valueOf(durationMs));

            log.info(
                    "HTTP {} {} -> {} ({}ms)",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs);

            MDC.clear();
        }
    }
}
