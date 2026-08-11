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
 * AgentExecution entity.
 * Records one execution invocation of a specific agent.
 */
@Entity
@Table(
    name = "agent_executions",
    indexes = {
        @Index(name = "idx_agent_exec_conversation", columnList = "conversation_id"),
        @Index(name = "idx_agent_exec_user", columnList = "user_id"),
        @Index(name = "idx_agent_exec_status", columnList = "user_id, status")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class AgentExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "agent_name", nullable = false, length = 255)
    private String agentName;

    @Column(name = "input", columnDefinition = "TEXT")
    private String input;

    @Column(name = "output", columnDefinition = "TEXT")
    private String output;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ExecutionStatus status = ExecutionStatus.PENDING;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSONB")
    private Map<String, Object> metadata;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AgentExecution() {}

    public AgentExecution(Conversation conversation, User user, String agentName, String input) {
        this.conversation = conversation;
        this.user = user;
        this.agentName = agentName;
        this.input = input;
        this.status = ExecutionStatus.PENDING;
    }

    public void markStarted() {
        this.status = ExecutionStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    public void markCompleted(String output) {
        this.status = ExecutionStatus.COMPLETED;
        this.output = output;
        this.completedAt = Instant.now();
        if (this.startedAt != null) {
            this.durationMs = this.completedAt.toEpochMilli() - this.startedAt.toEpochMilli();
        }
    }

    public void markFailed(String errorMessage) {
        this.status = ExecutionStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
        if (this.startedAt != null) {
            this.durationMs = this.completedAt.toEpochMilli() - this.startedAt.toEpochMilli();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }

    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }

    public ExecutionStatus getStatus() { return status; }
    public void setStatus(ExecutionStatus status) { this.status = status; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public enum ExecutionStatus {
        PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    }
}
