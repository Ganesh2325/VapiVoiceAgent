package com.voiceos.vapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voiceos.exception.VoiceOsException;
import com.voiceos.security.VoiceOsRequestContext;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiWebhookPayload;
import com.voiceos.vapi.service.VapiWebhookSecurityService;
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
    private final VapiWebhookSecurityService vapiWebhookSecurityService;
    private final ObjectMapper objectMapper;

    public VapiWebhookController(VapiWebhookService vapiWebhookService,
                                 VapiWebhookSecurityService vapiWebhookSecurityService,
                                 ObjectMapper objectMapper) {
        this.vapiWebhookService = vapiWebhookService;
        this.vapiWebhookSecurityService = vapiWebhookSecurityService;
        this.objectMapper = objectMapper;
    }

    @PostMapping({"/api/webhooks/vapi", "/api/v1/webhooks/vapi"})
    @Operation(summary = "Vapi Server Webhook Handler", description = "Processes tool-calls, assistant-requests, status-updates, and transcripts from Vapi")
    public ResponseEntity<?> handleVapiWebhook(
            @RequestHeader(value = "x-vapi-secret", required = false) String secretHeader,
            @RequestBody String rawPayload
    ) {
        log.debug("Received incoming Vapi webhook request. Length: {} chars requestId={}",
                rawPayload != null ? rawPayload.length() : 0,
                VoiceOsRequestContext.currentRequestId());

        vapiWebhookSecurityService.verify(secretHeader);

        try {
            VapiWebhookPayload payload = objectMapper.readValue(rawPayload, VapiWebhookPayload.class);
            Object response = vapiWebhookService.processWebhook(payload, rawPayload);
            return ResponseEntity.ok(response);
        } catch (VoiceOsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing Vapi webhook payload: {} requestId={}",
                    e.getClass().getSimpleName(), VoiceOsRequestContext.currentRequestId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid Vapi webhook payload"));
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
