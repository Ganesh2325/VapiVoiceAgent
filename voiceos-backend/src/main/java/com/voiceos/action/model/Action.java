package com.voiceos.action.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
 * Canonical persisted action for the VoiceOS execution pipeline.
 */
@Entity
@Table(
        name = "actions",
        indexes = {
                @Index(name = "idx_actions_user", columnList = "user_id"),
                @Index(name = "idx_actions_conversation", columnList = "conversation_id"),
                @Index(name = "idx_actions_call", columnList = "call_id"),
                @Index(name = "idx_actions_status", columnList = "status"),
                @Index(name = "idx_actions_idempotency", columnList = "idempotency_key", unique = true)
        }
)
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Action {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(name = "call_id", length = 255)
    private String callId;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "event_id", length = 255)
    private String eventId;

    @Column(name = "idempotency_key", unique = true, length = 500)
    private String idempotencyKey;

    @Column(name = "action_type", length = 255)
    private String actionType;

    @Column(name = "intent", columnDefinition = "TEXT")
    private String intent;

    @Column(name = "agent_name", length = 255)
    private String targetAgent;

    @Column(name = "tool_name", length = 255)
    private String toolName;

    @Column(name = "provider_name", length = 255)
    private String targetProvider;

    @Column(name = "provider_mode", length = 50)
    private String providerMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ActionStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "JSONB")
    private Map<String, Object> payload = new HashMap<>();

    @Column(name = "result", columnDefinition = "TEXT")
    private String result;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "verification_passed")
    private Boolean verificationPassed;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    public Action() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.status = ActionStatus.RECEIVED;
        this.payload = new HashMap<>();
    }

    public Action(UUID userId, String intent) {
        this();
        this.userId = userId;
        this.intent = intent;
        this.actionType = intent;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }

    public String getCallId() { return callId; }
    public void setCallId(String callId) { this.callId = callId; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public String getTargetAgent() { return targetAgent; }
    public void setTargetAgent(String targetAgent) { this.targetAgent = targetAgent; }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public String getTargetProvider() { return targetProvider; }
    public void setTargetProvider(String targetProvider) { this.targetProvider = targetProvider; }

    public String getProviderMode() { return providerMode; }
    public void setProviderMode(String providerMode) { this.providerMode = providerMode; }

    public ActionStatus getStatus() { return status; }
    public void setStatus(ActionStatus status) { this.status = status; }

    public Map<String, Object> getPayload() {
        if (payload == null) {
            payload = new HashMap<>();
        }
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload != null ? new HashMap<>(payload) : new HashMap<>();
    }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Boolean getVerificationPassed() { return verificationPassed; }
    public void setVerificationPassed(Boolean verificationPassed) { this.verificationPassed = verificationPassed; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
}
