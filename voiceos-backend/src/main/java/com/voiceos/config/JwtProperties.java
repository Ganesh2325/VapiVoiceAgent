package com.voiceos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT configuration properties.
 * Bound from the {@code voiceos.jwt.*} namespace.
 */
@ConfigurationProperties(prefix = "voiceos.jwt")
public record JwtProperties(
        String secret,
        long expirationMs,
        long refreshExpirationMs
) {}
