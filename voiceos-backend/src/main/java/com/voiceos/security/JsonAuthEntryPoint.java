package com.voiceos.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Returns a consistent JSON 401 for missing or invalid authentication.
 * Does not expose stack traces or token contents.
 */
@Component
public class JsonAuthEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(JsonAuthEntryPoint.class);

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.debug("Authentication required for {} {}: {}",
                request.getMethod(), request.getRequestURI(),
                authException != null ? authException.getClass().getSimpleName() : "none");

        if (response.isCommitted()) {
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String requestId = request.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER);
        response.getWriter().write(
                "{\"status\":401,\"error\":\"Unauthorized\",\"errorCode\":\"UNAUTHORIZED\","
                        + "\"message\":\"Authentication required\",\"path\":\""
                        + jsonEscape(request.getRequestURI()) + "\""
                        + (requestId != null ? ",\"requestId\":\"" + jsonEscape(requestId) + "\"" : "")
                        + "}"
        );
    }

    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
