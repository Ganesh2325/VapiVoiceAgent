package com.voiceos.vapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiWebhookPayload;
import com.voiceos.vapi.service.VapiWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for incoming Vapi Webhooks and Server Tool Calls.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /api/webhooks/vapi} — Primary Vapi Server URL</li>
 *   <li>{@code POST /api/v1/webhooks/vapi} — Versioned alias</li>
 *   <li>{@code GET /api/webhooks/vapi/health} — Webhook connectivity health check</li>
 * </ul>
 */
@RestController
@Tag(name = "Vapi Webhooks", description = "Endpoints handling Vapi voice agent tool calls and lifecycle events")
public class VapiWebhookController {

    private static final Logger log = LoggerFactory.getLogger(VapiWebhookController.class);

    private final VapiWebhookService vapiWebhookService;
    private final ObjectMapper objectMapper;

    public VapiWebhookController(VapiWebhookService vapiWebhookService, ObjectMapper objectMapper) {
        this.vapiWebhookService = vapiWebhookService;
        this.objectMapper = objectMapper;
    }

    @PostMapping({"/api/webhooks/vapi", "/api/v1/webhooks/vapi"})
    @Operation(summary = "Vapi Server Webhook Handler", description = "Processes tool-calls, assistant-requests, status-updates, and transcripts from Vapi")
    public ResponseEntity<?> handleVapiWebhook(
            @RequestHeader(value = "x-vapi-secret", required = false) String secretHeader,
            @RequestBody String rawPayload
    ) {
        log.debug("Received incoming Vapi webhook request. Length: {} chars", rawPayload.length());

        // Validate secret
        if (!vapiWebhookService.validateSecret(secretHeader)) {
            log.warn("Unauthorized Vapi webhook call: invalid or missing x-vapi-secret header.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid x-vapi-secret"));
        }

        try {
            VapiWebhookPayload payload = objectMapper.readValue(rawPayload, VapiWebhookPayload.class);
            Object response = vapiWebhookService.processWebhook(payload, rawPayload);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing Vapi webhook payload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process webhook: " + e.getMessage()));
        }
    }

    @GetMapping("/api/webhooks/vapi/health")
    @Operation(summary = "Vapi Webhook Health Check", description = "Verifies the Vapi webhook server is active and reachable")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "VoiceOS Vapi Integration",
                "timestamp", System.currentTimeMillis()
        ));
    }
}
