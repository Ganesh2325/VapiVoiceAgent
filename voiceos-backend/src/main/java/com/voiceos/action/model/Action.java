package com.voiceos.action.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a generic action to be executed (e.g. Booking a flight, Sending an email).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Action {
    private UUID id;
    private UUID userId;
    private String intent;
    private String targetAgent;
    private String targetProvider;
    private ActionStatus status;
    private Map<String, Object> payload;
    private String result;
    private Instant createdAt;
    private Instant completedAt;

    public Action() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.status = ActionStatus.REQUESTED;
    }

    public Action(UUID userId, String intent) {
        this();
        this.userId = userId;
        this.intent = intent;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    
    public String getTargetAgent() { return targetAgent; }
    public void setTargetAgent(String targetAgent) { this.targetAgent = targetAgent; }
    
    public String getTargetProvider() { return targetProvider; }
    public void setTargetProvider(String targetProvider) { this.targetProvider = targetProvider; }
    
    public ActionStatus getStatus() { return status; }
    public void setStatus(ActionStatus status) { this.status = status; }
    
    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }
    
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
