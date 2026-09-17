package com.voiceos.action.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "action_steps",
        uniqueConstraints = @UniqueConstraint(name = "uq_action_steps_order", columnNames = {"action_id", "sequence_no"}),
        indexes = {
                @Index(name = "idx_action_steps_action", columnList = "action_id"),
                @Index(name = "idx_action_steps_action_status", columnList = "action_id, status"),
                @Index(name = "idx_action_steps_request", columnList = "request_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class ActionStep {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "action_id", nullable = false)
    private UUID actionId;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, length = 50)
    private ActionStepType stepType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ActionStepStatus status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "agent_name", length = 255)
    private String agentName;

    @Column(name = "tool_name", length = 255)
    private String toolName;

    @Column(name = "provider_name", length = 255)
    private String providerName;

    @Column(name = "provider_mode", length = 50)
    private String providerMode;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", columnDefinition = "JSONB")
    private Map<String, Object> metadata = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_json", columnDefinition = "JSONB")
    private Map<String, Object> result = new HashMap<>();

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ActionStep() {
        this.id = UUID.randomUUID();
        this.status = ActionStepStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.metadata = new HashMap<>();
        this.result = new HashMap<>();
    }

    public void transitionTo(ActionStepStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException("Illegal action step transition " + status + " -> " + target
                    + " for step " + id);
        }
        this.status = target;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getActionId() { return actionId; }
    public void setActionId(UUID actionId) { this.actionId = actionId; }

    public int getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(int sequenceNo) { this.sequenceNo = sequenceNo; }

    public ActionStepType getStepType() { return stepType; }
    public void setStepType(ActionStepType stepType) { this.stepType = stepType; }

    public ActionStepStatus getStatus() { return status; }
    public void setStatus(ActionStepStatus status) { this.status = status; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getProviderMode() { return providerMode; }
    public void setProviderMode(String providerMode) { this.providerMode = providerMode; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public Map<String, Object> getMetadata() {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public Map<String, Object> getResult() {
        if (result == null) {
            result = new HashMap<>();
        }
        return result;
    }

    public void setResult(Map<String, Object> result) {
        this.result = result != null ? new HashMap<>(result) : new HashMap<>();
    }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
