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
 * Evaluation entity.
 * Stores quality metrics for conversations and agent executions.
 */
@Entity
@Table(
    name = "evaluations",
    indexes = {
        @Index(name = "idx_evaluation_conversation", columnList = "conversation_id"),
        @Index(name = "idx_evaluation_agent_exec", columnList = "agent_execution_id"),
        @Index(name = "idx_evaluation_created", columnList = "created_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_execution_id")
    private AgentExecution agentExecution;

    @Column(name = "task_success")
    private Boolean taskSuccess;

    @Column(name = "tool_success_rate")
    private Double toolSuccessRate;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "hallucination_score")
    private Double hallucinationScore;

    @Column(name = "human_corrected", nullable = false)
    private boolean humanCorrected = false;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSONB")
    private Map<String, Object> metadata;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Evaluation() {}

    public Evaluation(Conversation conversation, AgentExecution agentExecution) {
        this.conversation = conversation;
        this.agentExecution = agentExecution;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }

    public AgentExecution getAgentExecution() { return agentExecution; }
    public void setAgentExecution(AgentExecution agentExecution) { this.agentExecution = agentExecution; }

    public Boolean getTaskSuccess() { return taskSuccess; }
    public void setTaskSuccess(Boolean taskSuccess) { this.taskSuccess = taskSuccess; }

    public Double getToolSuccessRate() { return toolSuccessRate; }
    public void setToolSuccessRate(Double toolSuccessRate) { this.toolSuccessRate = toolSuccessRate; }

    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }

    public Double getHallucinationScore() { return hallucinationScore; }
    public void setHallucinationScore(Double hallucinationScore) { this.hallucinationScore = hallucinationScore; }

    public boolean isHumanCorrected() { return humanCorrected; }
    public void setHumanCorrected(boolean humanCorrected) { this.humanCorrected = humanCorrected; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
