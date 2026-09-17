package com.voiceos.vapi.service;

import com.voiceos.config.VoiceOsProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VapiWebhookSecurityServiceTest {

    @Test
    void validSecretAccepted() {
        VapiWebhookSecurityService service = service("correct-secret", true, false);
        assertDoesNotThrow(() -> service.verify("correct-secret"));
        assertTrue(service.isValid("correct-secret"));
    }

    @Test
    void invalidSecretRejected() {
        VapiWebhookSecurityService service = service("correct-secret", true, false);
        assertFalse(service.isValid("wrong-secret"));
        assertFalse(service.isValid(null));
        assertFalse(service.isValid(""));
    }

    @Test
    void missingSecretRejectedInSecureMode() {
        VapiWebhookSecurityService service = service("", true, false);
        assertFalse(service.isValid("anything"));
        assertFalse(service.isValid(null));
    }

    @Test
    void insecureLocalModeAllowsBlankSecret() {
        VapiWebhookSecurityService service = service("", true, true);
        assertTrue(service.isValid(null));
    }

    private static VapiWebhookSecurityService service(String secret, boolean require, boolean allowInsecure) {
        VoiceOsProperties properties = new VoiceOsProperties(
                null, null,
                new VoiceOsProperties.VoiceProperties("vapi",
                        new VoiceOsProperties.VapiProperties("k", "a", secret, "pk", "https://api.vapi.ai")),
                null,
                new VoiceOsProperties.WebhooksProperties(secret, null, null, require, allowInsecure),
                null, null, null, null
        );
        return new VapiWebhookSecurityService(properties);
    }
}
