package com.voiceos.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Memory entity.
 * Represents a single memory entry in the VoiceOS memory system.
 */
@Entity
@Table(
    name = "memory_entries",
    indexes = {
        @Index(name = "idx_memory_user_type", columnList = "user_id, memory_type"),
        @Index(name = "idx_memory_key", columnList = "user_id, key"),
        @Index(name = "idx_memory_expires", columnList = "expires_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class Memory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "memory_type", nullable = false, length = 50)
    private MemoryType memoryType;

    @Column(name = "key", length = 500)
    private String key;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "vector_id", length = 255)
    private String vectorId;

    @Column(name = "source", length = 500)
    private String source;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSONB")
    private Map<String, Object> metadata;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Memory() {}

    public Memory(User user, MemoryType memoryType, String key, String content) {
        this.user = user;
        this.memoryType = memoryType;
        this.key = key;
        this.content = content;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public MemoryType getMemoryType() { return memoryType; }
    public void setMemoryType(MemoryType memoryType) { this.memoryType = memoryType; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getVectorId() { return vectorId; }
    public void setVectorId(String vectorId) { this.vectorId = vectorId; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public enum MemoryType {
        SHORT_TERM, LONG_TERM, EPISODIC, SEMANTIC
    }
}
