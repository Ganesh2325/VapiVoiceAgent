package com.voiceos.config;

import com.voiceos.provider.ProviderBinding;

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
        AgentsProperties agents,
        ProvidersProperties providers
) {

    public VoiceOsProperties {
        if (providers == null) {
            providers = ProvidersProperties.defaults();
        }
    }

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
            String calendarSecret,
            Boolean requireSecret,
            Boolean allowInsecureLocal
    ) {
        public WebhooksProperties {
            if (requireSecret == null) {
                requireSecret = Boolean.TRUE;
            }
            if (allowInsecureLocal == null) {
                allowInsecureLocal = Boolean.FALSE;
            }
        }

        public boolean secretRequired() {
            return requireSecret == null || requireSecret;
        }

        public boolean insecureLocalAllowed() {
            return Boolean.TRUE.equals(allowInsecureLocal);
        }
    }

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
            int maxRetries,
            int maxToolCallsPerAction
    ) {
        public AgentsProperties {
            if (maxToolCallsPerAction <= 0) {
                maxToolCallsPerAction = 2;
            }
        }
    }

    /**
     * Explicit provider bindings. Missing credentials never cause a silent MOCK fallback
     * when mode is REAL.
     */
    public record ProvidersProperties(
            ProviderBinding calculator,
            ProviderBinding travel,
            ProviderBinding whatsapp,
            ProviderBinding email,
            ProviderBinding calendar,
            ProviderBinding payment
    ) {
        public static ProvidersProperties defaults() {
            return new ProvidersProperties(
                    ProviderBinding.realDefault(),
                    ProviderBinding.mockDefault(),
                    ProviderBinding.mockDefault(),
                    ProviderBinding.mockDefault(),
                    ProviderBinding.mockDefault(),
                    ProviderBinding.mockDefault()
            );
        }

        public ProvidersProperties {
            if (calculator == null) {
                calculator = ProviderBinding.realDefault();
            }
            if (travel == null) {
                travel = ProviderBinding.mockDefault();
            }
            if (whatsapp == null) {
                whatsapp = ProviderBinding.mockDefault();
            }
            if (email == null) {
                email = ProviderBinding.mockDefault();
            }
            if (calendar == null) {
                calendar = ProviderBinding.mockDefault();
            }
            if (payment == null) {
                payment = ProviderBinding.mockDefault();
            }
        }
    }
}
