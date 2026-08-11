package com.voiceos.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * AgentPlan entity.
 * Represents the Orchestrator's explicit execution plan for fulfilling a user goal.
 */
@Entity
@Table(
    name = "agent_plans",
    indexes = {
        @Index(name = "idx_agent_plan_conversation", columnList = "conversation_id"),
        @Index(name = "idx_agent_plan_user", columnList = "user_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class AgentPlan {

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

    @Column(name = "goal", nullable = false, columnDefinition = "TEXT")
    private String goal;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "steps", nullable = false, columnDefinition = "JSONB")
    private List<PlanStep> steps;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PlanStatus status = PlanStatus.PENDING;

    @Column(name = "current_step_index", nullable = false)
    private int currentStepIndex = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AgentPlan() {}

    public AgentPlan(Conversation conversation, User user, String goal, List<PlanStep> steps) {
        this.conversation = conversation;
        this.user = user;
        this.goal = goal;
        this.steps = steps;
        this.status = PlanStatus.PENDING;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public List<PlanStep> getSteps() { return steps; }
    public void setSteps(List<PlanStep> steps) { this.steps = steps; }

    public PlanStatus getStatus() { return status; }
    public void setStatus(PlanStatus status) { this.status = status; }

    public int getCurrentStepIndex() { return currentStepIndex; }
    public void setCurrentStepIndex(int currentStepIndex) { this.currentStepIndex = currentStepIndex; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public enum PlanStatus {
        PENDING, EXECUTING, COMPLETED, FAILED, CANCELLED
    }

    public record PlanStep(
            int stepIndex,
            String agentName,
            String action,
            String description,
            String status,
            String result
    ) {}
}
