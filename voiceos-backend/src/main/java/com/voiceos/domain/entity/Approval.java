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
 * Approval entity.
 * Represents a human-in-the-loop approval request for HIGH or CRITICAL actions.
 */
@Entity
@Table(
    name = "approvals",
    indexes = {
        @Index(name = "idx_approval_user_id", columnList = "user_id"),
        @Index(name = "idx_approval_user_status", columnList = "user_id, status"),
        @Index(name = "idx_approval_conversation", columnList = "conversation_id"),
        @Index(name = "idx_approval_expires", columnList = "expires_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_execution_id")
    private AgentExecution agentExecution;

    @Column(name = "tool_execution_id")
    private UUID toolExecutionId;

    @Column(name = "action_type", nullable = false, length = 255)
    private String actionType;

    @Column(name = "action_description", nullable = false, columnDefinition = "TEXT")
    private String actionDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 50)
    private ToolExecution.RiskLevel riskLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "JSONB")
    private Map<String, Object> payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Approval() {}

    public Approval(User user, Conversation conversation, AgentExecution agentExecution,
                    String actionType, String actionDescription, ToolExecution.RiskLevel riskLevel,
                    Map<String, Object> payload, Instant expiresAt) {
        this.user = user;
        this.conversation = conversation;
        this.agentExecution = agentExecution;
        this.actionType = actionType;
        this.actionDescription = actionDescription;
        this.riskLevel = riskLevel;
        this.payload = payload;
        this.expiresAt = expiresAt;
        this.status = ApprovalStatus.PENDING;
    }

    public void approve() {
        this.status = ApprovalStatus.APPROVED;
        this.respondedAt = Instant.now();
    }

    public void reject(String reason) {
        this.status = ApprovalStatus.REJECTED;
        this.rejectionReason = reason;
        this.respondedAt = Instant.now();
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }

    public AgentExecution getAgentExecution() { return agentExecution; }
    public void setAgentExecution(AgentExecution agentExecution) { this.agentExecution = agentExecution; }

    public UUID getToolExecutionId() { return toolExecutionId; }
    public void setToolExecutionId(UUID toolExecutionId) { this.toolExecutionId = toolExecutionId; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getActionDescription() { return actionDescription; }
    public void setActionDescription(String actionDescription) { this.actionDescription = actionDescription; }

    public ToolExecution.RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(ToolExecution.RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public ApprovalStatus getStatus() { return status; }
    public void setStatus(ApprovalStatus status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public Instant getRespondedAt() { return respondedAt; }
    public void setRespondedAt(Instant respondedAt) { this.respondedAt = respondedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public enum ApprovalStatus {
        PENDING, REQUIRES_AUTHENTICATION, REQUIRES_PAYMENT, APPROVED, REJECTED, EXPIRED
    }
}
