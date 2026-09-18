package com.voiceos.api.controller;

import com.voiceos.config.VoiceOsProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Browser-safe Vapi configuration for the authenticated console.
 * Never returns API keys, webhook secrets, or JWT material.
 */
@RestController
@RequestMapping("/api/v1/voice")
@Tag(name = "Voice Sessions", description = "Bind Vapi calls to authenticated VoiceOS users")
@SecurityRequirement(name = "bearerAuth")
public class VoiceConfigController {

    private final VoiceOsProperties properties;

    public VoiceConfigController(VoiceOsProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/config")
    @Operation(summary = "Return browser-safe Vapi public key and assistant id")
    public ResponseEntity<Map<String, Object>> config() {
        VoiceOsProperties.VapiProperties vapi = properties.voice() != null ? properties.voice().vapi() : null;
        String publicKey = vapi != null ? vapi.publicKey() : null;
        String assistantId = vapi != null ? vapi.assistantId() : null;
        boolean publicKeyPresent = notBlank(publicKey);
        boolean assistantIdPresent = notBlank(assistantId);
        boolean webhookSecretConfigured = webhookSecretConfigured();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("configured", publicKeyPresent && assistantIdPresent);
        body.put("publicKeyPresent", publicKeyPresent);
        body.put("assistantIdPresent", assistantIdPresent);
        body.put("webhookSecretConfigured", webhookSecretConfigured);
        if (publicKeyPresent) {
            body.put("publicKey", publicKey);
        }
        if (assistantIdPresent) {
            body.put("assistantId", assistantId);
        }
        return ResponseEntity.ok(body);
    }

    private boolean webhookSecretConfigured() {
        String webhookSecret = properties.webhooks() != null ? properties.webhooks().vapiSecret() : null;
        if (!notBlank(webhookSecret) && properties.voice() != null && properties.voice().vapi() != null) {
            webhookSecret = properties.voice().vapi().webhookSecret();
        }
        return notBlank(webhookSecret);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
