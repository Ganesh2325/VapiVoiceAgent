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
 * ToolExecution entity.
 * Records one invocation of a tool within an agent execution.
 */
@Entity
@Table(
    name = "tool_executions",
    indexes = {
        @Index(name = "idx_tool_exec_agent", columnList = "agent_execution_id"),
        @Index(name = "idx_tool_exec_approval", columnList = "approval_id"),
        @Index(name = "idx_tool_exec_status", columnList = "status")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class ToolExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_execution_id")
    private AgentExecution agentExecution;

    @Column(name = "tool_name", nullable = false, length = 255)
    private String toolName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input", columnDefinition = "JSONB")
    private Map<String, Object> input;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output", columnDefinition = "JSONB")
    private Map<String, Object> output;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ToolExecutionStatus status = ToolExecutionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 50)
    private RiskLevel riskLevel = RiskLevel.LOW;

    @Column(name = "requires_approval", nullable = false)
    private boolean requiresApproval = false;

    @Column(name = "approval_id")
    private UUID approvalId;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ToolExecution() {}

    public ToolExecution(AgentExecution agentExecution, String toolName,
                         Map<String, Object> input, RiskLevel riskLevel) {
        this.agentExecution = agentExecution;
        this.toolName = toolName;
        this.input = input;
        this.riskLevel = riskLevel;
        this.requiresApproval = riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL;
        this.status = requiresApproval ? ToolExecutionStatus.AWAITING_APPROVAL : ToolExecutionStatus.PENDING;
    }

    public void markCompleted(Map<String, Object> output) {
        this.status = ToolExecutionStatus.COMPLETED;
        this.output = output;
        this.completedAt = Instant.now();
        if (this.startedAt != null) {
            this.durationMs = this.completedAt.toEpochMilli() - this.startedAt.toEpochMilli();
        }
    }

    public void markFailed(String errorMessage) {
        this.status = ToolExecutionStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public AgentExecution getAgentExecution() { return agentExecution; }
    public void setAgentExecution(AgentExecution agentExecution) { this.agentExecution = agentExecution; }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public Map<String, Object> getInput() { return input; }
    public void setInput(Map<String, Object> input) { this.input = input; }

    public Map<String, Object> getOutput() { return output; }
    public void setOutput(Map<String, Object> output) { this.output = output; }

    public ToolExecutionStatus getStatus() { return status; }
    public void setStatus(ToolExecutionStatus status) { this.status = status; }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public boolean isRequiresApproval() { return requiresApproval; }
    public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }

    public UUID getApprovalId() { return approvalId; }
    public void setApprovalId(UUID approvalId) { this.approvalId = approvalId; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public enum ToolExecutionStatus {
        PENDING, RUNNING, COMPLETED, FAILED, AWAITING_APPROVAL, REJECTED
    }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
