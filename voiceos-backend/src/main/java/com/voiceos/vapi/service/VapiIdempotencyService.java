package com.voiceos.vapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voiceos.domain.entity.WebhookEvent;
import com.voiceos.domain.repository.WebhookEventRepository;
import com.voiceos.exception.VoiceOsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Database-backed idempotency for Vapi webhook events.
 *
 * <p>The unique constraint on {@code webhook_events.idempotency_key} is the
 * concurrency boundary: only one request can insert a given key. Inserts run
 * in a dedicated transaction so a unique-constraint race rolls back only that
 * insert, not the caller.
 */
@Service
public class VapiIdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(VapiIdempotencyService.class);
    private static final int IN_PROGRESS_WAIT_MS = 150;
    private static final int IN_PROGRESS_RETRIES = 8;

    private final WebhookEventRepository webhookEventRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate requiresNew;

    public VapiIdempotencyService(WebhookEventRepository webhookEventRepository,
                                  ObjectMapper objectMapper) {
        this(webhookEventRepository, objectMapper, null);
    }

    @Autowired
    public VapiIdempotencyService(WebhookEventRepository webhookEventRepository,
                                  ObjectMapper objectMapper,
                                  PlatformTransactionManager transactionManager) {
        this.webhookEventRepository = webhookEventRepository;
        this.objectMapper = objectMapper;
        if (transactionManager != null) {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            this.requiresNew = template;
        } else {
            this.requiresNew = null;
        }
    }

    public record Claim(Kind kind, WebhookEvent event, Object cachedResponse) {
        public enum Kind { FRESH, DUPLICATE, IN_PROGRESS }

        public boolean isFresh() {
            return kind == Kind.FRESH;
        }

        public boolean isDuplicate() {
            return kind == Kind.DUPLICATE;
        }
    }

    /**
     * Claims the right to process this event, or returns the stored result.
     */
    public Claim claim(String idempotencyKey, String eventType, String callId, String eventId,
                       String requestId, Map<String, Object> payload) {
        Optional<WebhookEvent> existing = webhookEventRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return toClaim(existing.get());
        }

        WebhookEvent event = new WebhookEvent("VAPI", eventType, payload, null, idempotencyKey);
        event.setCallId(callId);
        event.setEventId(eventId);
        event.setRequestId(requestId);
        try {
            WebhookEvent saved = inNewTransaction(() -> webhookEventRepository.saveAndFlush(event));
            log.info("Idempotency claimed keyHash={} eventType={} callId={} requestId={}",
                    Integer.toHexString(idempotencyKey.hashCode()), eventType, callId, requestId);
            return new Claim(Claim.Kind.FRESH, saved, null);
        } catch (RuntimeException ex) {
            if (!isDuplicateConstraint(ex)) {
                throw ex;
            }
            WebhookEvent winner = webhookEventRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> VoiceOsException.conflict("Duplicate webhook event is being processed"));
            return toClaim(winner);
        }
    }

    public Claim awaitDuplicateOrConflict(String idempotencyKey) {
        for (int i = 0; i < IN_PROGRESS_RETRIES; i++) {
            Optional<WebhookEvent> existing = webhookEventRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent() && existing.get().isProcessed() && existing.get().getResponseJson() != null) {
                return toClaim(existing.get());
            }
            try {
                Thread.sleep(IN_PROGRESS_WAIT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw VoiceOsException.conflict("Duplicate webhook event is already being processed");
    }

    public void complete(WebhookEvent event, Object response) {
        inNewTransaction(() -> {
            try {
                event.markProcessed();
                event.setResponseJson(objectMapper.writeValueAsString(response));
                return webhookEventRepository.saveAndFlush(event);
            } catch (JsonProcessingException e) {
                event.markProcessed();
                event.setResponseJson("{\"status\":\"acknowledged\"}");
                webhookEventRepository.saveAndFlush(event);
                log.warn("Could not serialize webhook response for idempotency storage: {}", e.getClass().getSimpleName());
                return event;
            }
        });
    }

    public void fail(WebhookEvent event, String errorMessage) {
        inNewTransaction(() -> {
            event.markFailed(errorMessage);
            return webhookEventRepository.saveAndFlush(event);
        });
    }

    public Object deserializeResponse(String responseJson) {
        if (responseJson == null || responseJson.isBlank()) {
            return Map.of("status", "acknowledged", "duplicate", true);
        }
        try {
            return objectMapper.readValue(responseJson, Object.class);
        } catch (JsonProcessingException e) {
            return Map.of("status", "acknowledged", "duplicate", true);
        }
    }

    public boolean isDuplicateConstraint(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof DataIntegrityViolationException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toUpperCase();
                if (normalized.contains("IDEMPOTENCY_KEY")
                        || normalized.contains("UNIQUE INDEX")
                        || normalized.contains("DUPLICATE KEY")
                        || normalized.contains("CONSTRAINT_INDEX")
                        || normalized.contains("UNIQUE CONSTRAINT")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private Claim toClaim(WebhookEvent event) {
        if (event.isProcessed() && event.getResponseJson() != null) {
            return new Claim(Claim.Kind.DUPLICATE, event, deserializeResponse(event.getResponseJson()));
        }
        return new Claim(Claim.Kind.IN_PROGRESS, event, null);
    }

    private <T> T inNewTransaction(Supplier<T> action) {
        if (requiresNew == null) {
            return action.get();
        }
        return requiresNew.execute(status -> action.get());
    }
}
