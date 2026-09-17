package com.voiceos.vapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voiceos.domain.entity.WebhookEvent;
import com.voiceos.security.RequestCorrelationFilter;
import com.voiceos.security.VoiceOsRequestContext;
import com.voiceos.vapi.dto.VapiWebhookDTOs.*;
import com.voiceos.voice.VoiceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
    private final VapiWebhookSecurityService vapiWebhookSecurityService;
    private final VapiIdempotencyService vapiIdempotencyService;
    private final VapiIdempotencyKeyBuilder idempotencyKeyBuilder;
    private final ObjectMapper objectMapper;
    private final VoiceConfiguration voiceConfiguration;

    public VapiWebhookService(
            VapiToolService vapiToolService,
            VapiEventProcessor vapiEventProcessor,
            VapiWebhookSecurityService vapiWebhookSecurityService,
            VapiIdempotencyService vapiIdempotencyService,
            VapiIdempotencyKeyBuilder idempotencyKeyBuilder,
            ObjectMapper objectMapper,
            VoiceConfiguration voiceConfiguration
    ) {
        this.vapiToolService = vapiToolService;
        this.vapiEventProcessor = vapiEventProcessor;
        this.vapiWebhookSecurityService = vapiWebhookSecurityService;
        this.vapiIdempotencyService = vapiIdempotencyService;
        this.idempotencyKeyBuilder = idempotencyKeyBuilder;
        this.objectMapper = objectMapper;
        this.voiceConfiguration = voiceConfiguration;
    }

    /**
     * Validates secret header from Vapi. Fail-closed unless local insecure mode is explicit.
     */
    public boolean validateSecret(String secretHeader) {
        return vapiWebhookSecurityService.isValid(secretHeader);
    }

    /**
     * Processes inbound Vapi webhook payload with stable database-backed idempotency.
     */
    @SuppressWarnings("unchecked")
    public Object processWebhook(VapiWebhookPayload payload, String rawPayload) {
        if (payload == null || payload.message() == null) {
            log.warn("Received empty Vapi webhook message. requestId={}", VoiceOsRequestContext.currentRequestId());
            return Map.of("status", "ok");
        }

        VapiMessage message = payload.message();
        VapiCall call = payload.call() != null ? payload.call() : message.call();
        String eventType = message.type() != null ? message.type().toLowerCase() : "unknown";
        String callId = call != null ? call.id() : null;
        String eventId = message.id();
        String requestId = VoiceOsRequestContext.currentRequestId();

        if (callId != null) {
            MDC.put(RequestCorrelationFilter.MDC_CALL_ID, callId);
        }
        if (eventId != null) {
            MDC.put(RequestCorrelationFilter.MDC_EVENT_ID, eventId);
        }
        VoiceOsRequestContext current = VoiceOsRequestContext.current();
        if (current != null) {
            VoiceOsRequestContext.set(current.withCall(callId, eventId));
        }

        log.info("Processing Vapi webhook eventType={} callId={} eventId={} requestId={}",
                eventType, callId, eventId, requestId);

        String idempotencyKey = idempotencyKeyBuilder.build(payload);
        Map<String, Object> storedPayload;
        try {
            storedPayload = objectMapper.convertValue(payload, Map.class);
        } catch (IllegalArgumentException e) {
            storedPayload = Map.of("eventType", eventType);
        }

        VapiIdempotencyService.Claim claim;
        try {
            claim = vapiIdempotencyService.claim(
                    idempotencyKey, eventType, callId, eventId, requestId, storedPayload);
        } catch (RuntimeException ex) {
            if (!vapiIdempotencyService.isDuplicateConstraint(ex)) {
                throw ex;
            }
            claim = vapiIdempotencyService.awaitDuplicateOrConflict(idempotencyKey);
        }

        if (claim.isDuplicate()) {
            log.info("Duplicate Vapi webhook ignored eventType={} callId={} eventId={} requestId={}",
                    eventType, callId, eventId, requestId);
            return claim.cachedResponse();
        }
        if (claim.kind() == VapiIdempotencyService.Claim.Kind.IN_PROGRESS) {
            VapiIdempotencyService.Claim waited = vapiIdempotencyService.awaitDuplicateOrConflict(idempotencyKey);
            log.info("Concurrent Vapi webhook resolved as duplicate eventType={} callId={} requestId={}",
                    eventType, callId, requestId);
            return waited.cachedResponse();
        }

        WebhookEvent event = claim.event();
        try {
            Object result = route(eventType, message, call);
            vapiIdempotencyService.complete(event, result);
            return result;
        } catch (RuntimeException e) {
            vapiIdempotencyService.fail(event, e.getClass().getSimpleName());
            throw e;
        }
    }

    private Object route(String eventType, VapiMessage message, VapiCall call) {
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
