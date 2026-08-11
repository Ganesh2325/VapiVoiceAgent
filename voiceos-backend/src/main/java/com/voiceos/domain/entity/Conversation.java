package com.voiceos.domain.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Conversation entity.
 * Represents a single conversation session between a user and VoiceOS agents.
 */
@Entity
@Table(
    name = "conversations",
    indexes = {
        @Index(name = "idx_conversations_user_id", columnList = "user_id"),
        @Index(name = "idx_conversations_status", columnList = "user_id, status")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", length = 500)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ConversationStatus status = ConversationStatus.ACTIVE;

    @Column(name = "voice_session_id", length = 255)
    private String voiceSessionId;

    @Column(name = "active_agent_name", length = 255)
    private String activeAgentName;

    @Column(name = "message_count", nullable = false)
    private int messageCount = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Conversation() {}

    public Conversation(User user, String title) {
        this.user = user;
        this.title = title;
        this.status = ConversationStatus.ACTIVE;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public ConversationStatus getStatus() { return status; }
    public void setStatus(ConversationStatus status) { this.status = status; }

    public String getVoiceSessionId() { return voiceSessionId; }
    public void setVoiceSessionId(String voiceSessionId) { this.voiceSessionId = voiceSessionId; }

    public String getActiveAgentName() { return activeAgentName; }
    public void setActiveAgentName(String activeAgentName) { this.activeAgentName = activeAgentName; }

    public int getMessageCount() { return messageCount; }
    public void setMessageCount(int messageCount) { this.messageCount = messageCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public enum ConversationStatus {
        ACTIVE, PAUSED, COMPLETED, ARCHIVED
    }
}
