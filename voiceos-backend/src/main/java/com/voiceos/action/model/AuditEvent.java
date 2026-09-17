package com.voiceos.action.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Append-only evidence of an action transition. Application APIs never update or delete rows.
 */
@Entity
@Table(
        name = "audit_events",
        indexes = {
                @Index(name = "idx_audit_events_action", columnList = "action_id, occurred_at"),
                @Index(name = "idx_audit_events_step", columnList = "action_step_id"),
                @Index(name = "idx_audit_events_request", columnList = "request_id"),
                @Index(name = "idx_audit_events_key", columnList = "event_key", unique = true)
        }
)
@EntityListeners(AuditingEntityListener.class)
public class AuditEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "action_id", nullable = false)
    private UUID actionId;

    @Column(name = "action_step_id")
    private UUID actionStepId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 80)
    private AuditEventType eventType;

    @Column(name = "event_key", nullable = false, unique = true, length = 500)
    private String eventKey;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "call_id", length = 255)
    private String callId;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 50)
    private AuditActorType actorType;

    @Column(name = "actor_id", length = 255)
    private String actorId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", columnDefinition = "JSONB")
    private Map<String, Object> metadata = new HashMap<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AuditEvent() {
        this.id = UUID.randomUUID();
        this.occurredAt = Instant.now();
        this.createdAt = this.occurredAt;
        this.metadata = new HashMap<>();
        this.actorType = AuditActorType.SYSTEM;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getActionId() { return actionId; }
    public void setActionId(UUID actionId) { this.actionId = actionId; }

    public UUID getActionStepId() { return actionStepId; }
    public void setActionStepId(UUID actionStepId) { this.actionStepId = actionStepId; }

    public AuditEventType getEventType() { return eventType; }
    public void setEventType(AuditEventType eventType) { this.eventType = eventType; }

    public String getEventKey() { return eventKey; }
    public void setEventKey(String eventKey) { this.eventKey = eventKey; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getCallId() { return callId; }
    public void setCallId(String callId) { this.callId = callId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public AuditActorType getActorType() { return actorType; }
    public void setActorType(AuditActorType actorType) { this.actorType = actorType; }

    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }

    public Map<String, Object> getMetadata() {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
