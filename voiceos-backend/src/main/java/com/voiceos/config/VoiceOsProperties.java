package com.voiceos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Root VoiceOS application properties.
 * Bound from the {@code voiceos.*} namespace in application.yml.
 */
@ConfigurationProperties(prefix = "voiceos")
public record VoiceOsProperties(
        AiProperties ai,
        JwtProperties jwt,
        VoiceProperties voice,
        SecurityProperties security,
        WebhooksProperties webhooks,
        IntegrationsProperties integrations,
        RagProperties rag,
        AgentsProperties agents
) {

    public record AiProperties(
            String provider,
            boolean mock,
            RetryProperties retry
    ) {}

    public record RetryProperties(
            int maxAttempts,
            long initialDelayMs,
            double multiplier
    ) {}

    public record JwtProperties(
            String secret,
            long expirationMs,
            long refreshExpirationMs
    ) {}

    public record VoiceProperties(
            String provider,
            VapiProperties vapi
    ) {}

    public record VapiProperties(
            String apiKey,
            String assistantId,
            String webhookSecret,
            String publicKey,
            String baseUrl
    ) {}

    public record SecurityProperties(
            CorsProperties cors,
            RateLimitProperties rateLimit
    ) {}

    public record CorsProperties(
            List<String> allowedOrigins
    ) {}

    public record RateLimitProperties(
            boolean enabled,
            int requestsPerMinute
    ) {}

    public record WebhooksProperties(
            String vapiSecret,
            String githubSecret,
            String calendarSecret
    ) {}

    public record IntegrationsProperties(
            GithubProperties github,
            SmtpProperties smtp
    ) {}

    public record GithubProperties(String token) {}

    public record SmtpProperties(
            String host,
            int port,
            String username,
            String password
    ) {}

    public record RagProperties(
            int chunkSize,
            int chunkOverlap,
            int topK
    ) {}

    public record AgentsProperties(
            long executionTimeoutMs,
            int maxRetries
    ) {}
}
