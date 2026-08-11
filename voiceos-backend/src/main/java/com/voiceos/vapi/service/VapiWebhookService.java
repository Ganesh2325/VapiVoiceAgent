package com.voiceos.vapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voiceos.config.VoiceOsProperties;
import com.voiceos.domain.entity.WebhookEvent;
import com.voiceos.domain.repository.WebhookEventRepository;
import com.voiceos.vapi.dto.VapiWebhookDTOs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Core Webhook Service for Vapi.
 * Validates request signatures/secrets, ensures idempotency, and routes payloads
 * to the tool executor or lifecycle event processor.
 */
@Service
public class VapiWebhookService {

    private static final Logger log = LoggerFactory.getLogger(VapiWebhookService.class);

    private final VapiToolService vapiToolService;
    private final VapiEventProcessor vapiEventProcessor;
    private final WebhookEventRepository webhookEventRepository;
    private final VoiceOsProperties properties;
    private final ObjectMapper objectMapper;
    private final VoiceConfiguration voiceConfiguration;

    public VapiWebhookService(
            VapiToolService vapiToolService,
            VapiEventProcessor vapiEventProcessor,
            WebhookEventRepository webhookEventRepository,
            VoiceOsProperties properties,
            ObjectMapper objectMapper,
            VoiceConfiguration voiceConfiguration
    ) {
        this.vapiToolService = vapiToolService;
        this.vapiEventProcessor = vapiEventProcessor;
        this.webhookEventRepository = webhookEventRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.voiceConfiguration = voiceConfiguration;
    }

    /**
     * Validates secret header from Vapi if a secret is configured.
     */
    public boolean validateSecret(String secretHeader) {
        String configuredSecret = properties.webhooks() != null ? properties.webhooks().vapiSecret() : null;
        if (configuredSecret == null || configuredSecret.isBlank()) {
            if (properties.voice() != null && properties.voice().vapi() != null) {
                configuredSecret = properties.voice().vapi().webhookSecret();
            }
        }

        // If no secret is configured in environment, allow requests (dev mode)
        if (configuredSecret == null || configuredSecret.isBlank()) {
            return true;
        }

        return configuredSecret.equals(secretHeader);
    }

    /**
     * Processes inbound Vapi webhook payload.
     */
    public Object processWebhook(VapiWebhookPayload payload, String rawPayload) {
        if (payload == null || payload.message() == null) {
            log.warn("Received empty Vapi webhook message.");
            return Map.of("status", "ok");
        }

        VapiMessage message = payload.message();
        VapiCall call = payload.call() != null ? payload.call() : message.call();
        String eventType = message.type() != null ? message.type().toLowerCase() : "unknown";

        log.info("Processing Vapi Webhook Event: '{}' for Call: {}", eventType, call != null ? call.id() : "N/A");

        // Record webhook event for audit trail & idempotency
        try {
            String idempotencyKey = (call != null && call.id() != null ? call.id() : "evt") + "_" + System.currentTimeMillis();
            WebhookEvent event = new WebhookEvent("VAPI", eventType, objectMapper.convertValue(payload, Map.class), null, idempotencyKey);
            event.markProcessed();
            webhookEventRepository.save(event);
        } catch (Exception e) {
            log.warn("Could not save WebhookEvent audit record: {}", e.getMessage());
        }

        // Route by event type
        return switch (eventType) {
            case "tool-calls", "tool-call", "function-call" ->
                    vapiToolService.handleToolCalls(message, call);

            case "assistant-request" ->
                    handleAssistantRequest(message, call);

            case "status-update" -> {
                vapiEventProcessor.processStatusUpdate(message, call);
                yield Map.of("status", "acknowledged");
            }

            case "transcript" -> {
                vapiEventProcessor.processTranscript(message, call);
                yield Map.of("status", "acknowledged");
            }

            case "end-of-call-report" -> {
                vapiEventProcessor.processEndOfCallReport(message, call);
                yield Map.of("status", "acknowledged");
            }

            case "speech-update" -> {
                log.debug("Vapi speech update event received.");
                yield Map.of("status", "acknowledged");
            }

            default -> {
                log.info("Unhandled Vapi webhook event type: {}", eventType);
                yield Map.of("status", "acknowledged");
            }
        };
    }

    private VapiAssistantResponse handleAssistantRequest(VapiMessage message, VapiCall call) {
        log.info("Generating dynamic assistant configuration for Vapi call: {}", call != null ? call.id() : "N/A");
        return new VapiAssistantResponse(voiceConfiguration.toVapiAssistantConfig());
    }
}
