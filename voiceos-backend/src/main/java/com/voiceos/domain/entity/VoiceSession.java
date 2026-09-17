package com.voiceos.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Binds a Vapi call to a VoiceOS user.
 *
 * <p>Identity is established only by an authenticated JWT bind. Customer email
 * on a Vapi payload is never treated as authentication.
 */
@Entity
@Table(
        name = "voice_sessions",
        indexes = {
                @Index(name = "idx_voice_sessions_call", columnList = "call_id", unique = true),
                @Index(name = "idx_voice_sessions_user", columnList = "user_id"),
                @Index(name = "idx_voice_sessions_status", columnList = "status")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class VoiceSession {

    public enum Status {
        UNBOUND,
        BOUND,
        EXPIRED,
        ENDED
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "call_id", nullable = false, unique = true, length = 255)
    private String callId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private Status status = Status.UNBOUND;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "bound_at")
    private Instant boundAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    public VoiceSession() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.status = Status.UNBOUND;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCallId() { return callId; }
    public void setCallId(String callId) { this.callId = callId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getBoundAt() { return boundAt; }
    public void setBoundAt(Instant boundAt) { this.boundAt = boundAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }

    public boolean isBoundAndActive() {
        return status == Status.BOUND
                && user != null
                && expiresAt != null
                && Instant.now().isBefore(expiresAt);
    }
}
