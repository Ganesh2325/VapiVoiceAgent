package com.voiceos.vapi.service;

import com.voiceos.config.VoiceOsProperties;
import com.voiceos.exception.VoiceOsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Verifies inbound Vapi webhook authenticity using the shared {@code x-vapi-secret}.
 *
 * <p>This is the project's existing Vapi mechanism (header secret). VoiceOS does
 * not invent additional unsigned HMAC schemes.
 *
 * <p>Fail-closed: a missing webhook secret rejects the request unless
 * {@code voiceos.webhooks.allow-insecure-local} is explicitly enabled for
 * documented local development.
 */
@Service
public class VapiWebhookSecurityService {

    private static final Logger log = LoggerFactory.getLogger(VapiWebhookSecurityService.class);

    private final VoiceOsProperties properties;

    public VapiWebhookSecurityService(VoiceOsProperties properties) {
        this.properties = properties;
    }

    /**
     * @throws VoiceOsException UNAUTHORIZED when the request must be rejected
     */
    public void verify(String secretHeader) {
        String configuredSecret = configuredSecret();
        boolean secretConfigured = configuredSecret != null && !configuredSecret.isBlank();
        boolean requireSecret = properties.webhooks() == null || properties.webhooks().secretRequired();
        boolean allowInsecureLocal = properties.webhooks() != null && properties.webhooks().insecureLocalAllowed();

        if (!secretConfigured) {
            if (requireSecret && !allowInsecureLocal) {
                log.warn("Vapi webhook rejected: webhook secret is not configured (fail-closed)");
                throw VoiceOsException.unauthorized("Vapi webhook secret is not configured");
            }
            if (allowInsecureLocal) {
                log.warn("Vapi webhook accepted without a configured secret because allow-insecure-local=true (local development only)");
                return;
            }
            log.warn("Vapi webhook rejected: webhook secret missing");
            throw VoiceOsException.unauthorized("Vapi webhook secret is not configured");
        }

        if (secretHeader == null || secretHeader.isBlank()) {
            log.warn("Vapi webhook rejected: missing x-vapi-secret header");
            throw VoiceOsException.unauthorized("Missing x-vapi-secret");
        }

        if (!constantTimeEquals(configuredSecret, secretHeader)) {
            log.warn("Vapi webhook rejected: invalid x-vapi-secret header");
            throw VoiceOsException.unauthorized("Invalid x-vapi-secret");
        }
    }

    /**
     * Boolean wrapper used by unit tests. Does not log the secret.
     */
    public boolean isValid(String secretHeader) {
        try {
            verify(secretHeader);
            return true;
        } catch (VoiceOsException ex) {
            return false;
        }
    }

    String configuredSecret() {
        String configuredSecret = properties.webhooks() != null ? properties.webhooks().vapiSecret() : null;
        if (configuredSecret == null || configuredSecret.isBlank()) {
            if (properties.voice() != null && properties.voice().vapi() != null) {
                configuredSecret = properties.voice().vapi().webhookSecret();
            }
        }
        return configuredSecret;
    }

    static boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        byte[] a = left.getBytes(StandardCharsets.UTF_8);
        byte[] b = right.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}
