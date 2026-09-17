package com.voiceos.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * WebhookEvent entity.
 * Records every incoming webhook event for idempotency, auditing, and replay.
 */
@Entity
@Table(
    name = "webhook_events",
    indexes = {
        @Index(name = "idx_webhook_idempotency", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_webhook_provider_processed", columnList = "provider, processed"),
        @Index(name = "idx_webhook_event_type", columnList = "provider, event_type")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "provider", nullable = false, length = 100)
    private String provider;

    @Column(name = "event_type", nullable = false, length = 255)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "JSONB")
    private Map<String, Object> payload;

    @Column(name = "signature", length = 500)
    private String signature;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 500)
    private String idempotencyKey;

    @Column(name = "processed", nullable = false)
    private boolean processed = false;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "response_json", columnDefinition = "TEXT")
    private String responseJson;

    @Column(name = "call_id", length = 255)
    private String callId;

    @Column(name = "event_id", length = 255)
    private String eventId;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public WebhookEvent() {}

    public WebhookEvent(String provider, String eventType,
                        Map<String, Object> payload, String signature, String idempotencyKey) {
        this.provider = provider;
        this.eventType = eventType;
        this.payload = payload;
        this.signature = signature;
        this.idempotencyKey = idempotencyKey;
        this.processed = false;
    }

    public void markProcessed() {
        this.processed = true;
        this.processedAt = Instant.now();
    }

    public void markFailed(String errorMessage) {
        this.errorMessage = errorMessage;
        this.processedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public boolean isProcessed() { return processed; }
    public void setProcessed(boolean processed) { this.processed = processed; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getResponseJson() { return responseJson; }
    public void setResponseJson(String responseJson) { this.responseJson = responseJson; }

    public String getCallId() { return callId; }
    public void setCallId(String callId) { this.callId = callId; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
