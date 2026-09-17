package com.voiceos.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Emits a single structured security/audit log line per HTTP request.
 *
 * <p>Logged: requestId, user (email only), method, path, status, duration.
 * Never logs Authorization headers, JWT values, passwords, API keys, or bodies.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class RequestAuditFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestAuditFilter.class);

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        long started = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - started) / 1_000_000L;
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String user = "anonymous";
            if (authentication != null
                    && authentication.isAuthenticated()
                    && authentication.getPrincipal() != null
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                user = authentication.getName();
            }
            log.info(
                    "http method={} path={} status={} durationMs={} user={} requestId={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs,
                    user,
                    MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID)
            );
        }
    }
}
