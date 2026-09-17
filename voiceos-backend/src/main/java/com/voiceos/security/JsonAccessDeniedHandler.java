package com.voiceos.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Returns a consistent JSON 403 for authenticated-but-unauthorized requests.
 */
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(JsonAccessDeniedHandler.class);

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("Access denied for {} {}", request.getMethod(), request.getRequestURI());
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"status\":403,\"error\":\"Forbidden\",\"errorCode\":\"FORBIDDEN\","
                        + "\"message\":\"You do not have permission to perform this action\",\"path\":\""
                        + request.getRequestURI().replace("\\", "\\\\").replace("\"", "\\\"")
                        + "\"}"
        );
    }
}
