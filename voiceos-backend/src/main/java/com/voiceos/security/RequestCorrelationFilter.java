package com.voiceos.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Assigns a request correlation id to every HTTP request.
 *
 * <p>Accepts an incoming {@code X-Request-Id} or generates a UUID. The value is
 * stored in MDC and {@link VoiceOsRequestContext} and echoed on the response.
 * Secrets and request bodies are never logged here.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_USER_ID = "userId";
    public static final String MDC_CALL_ID = "callId";
    public static final String MDC_EVENT_ID = "eventId";
    public static final String MDC_ACTION_ID = "actionId";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_REQUEST_ID, requestId);
        VoiceOsRequestContext.set(new VoiceOsRequestContext(requestId, null, null, null, null));
        response.setHeader(REQUEST_ID_HEADER, requestId);

        long started = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - started) / 1_000_000L;
            request.setAttribute("voiceos.durationMs", durationMs);
            VoiceOsRequestContext.clear();
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_CALL_ID);
            MDC.remove(MDC_EVENT_ID);
            MDC.remove(MDC_ACTION_ID);
        }
    }
}
