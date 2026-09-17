package com.voiceos.tool.impl;

import com.voiceos.domain.entity.Memory;
import com.voiceos.domain.entity.User;
import com.voiceos.domain.repository.MemoryRepository;
import com.voiceos.domain.repository.UserRepository;
import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Memory Save Tool for persisting user facts and preferences to long-term memory.
 * Risk Level: LOW.
 */
@Component
public class MemorySaveTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(MemorySaveTool.class);

    private final MemoryRepository memoryRepository;
    private final UserRepository userRepository;

    public MemorySaveTool(MemoryRepository memoryRepository, UserRepository userRepository) {
        this.memoryRepository = memoryRepository;
        this.userRepository = userRepository;
    }

    @Override
    public String getName() {
        return "memory_save";
    }

    @Override
    public String getDescription() {
        return "Persists a key-value fact or preference to user's long-term memory.";
    }

    @Override
    public ToolRiskLevel getRiskLevel() {
        return ToolRiskLevel.LOW;
    }

    @Override
    public void validateInput(Map<String, Object> params) {
        Tool.super.validateInput(params);
        if (!params.containsKey("content") || params.get("content") == null) {
            throw new IllegalArgumentException("Memory content cannot be empty.");
        }
    }

    @Override
    @Transactional
    public ToolResult execute(Map<String, Object> params) {
        long startMs = System.currentTimeMillis();
        String content = params.get("content").toString();
        String key = (String) params.getOrDefault("key", "preference");

        UUID userId = null;
        if (params.containsKey("userId") && params.get("userId") != null) {
            userId = UUID.fromString(params.get("userId").toString());
        }

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }
        if (user == null) {
            return ToolResult.failure("Authenticated userId is required to save memory.", System.currentTimeMillis() - startMs);
        }

        Memory memory = new Memory(user, Memory.MemoryType.LONG_TERM, key, content);
        memory = memoryRepository.save(memory);
        log.info("Saved long-term memory (id={}) for user {}: key='{}'", memory.getId(), user.getEmail(), key);

        long latency = System.currentTimeMillis() - startMs;
        return ToolResult.success(
                Map.of("memoryId", memory.getId().toString(), "key", key, "content", content),
                "Memory saved successfully: \"" + content + "\"",
                latency
        );
    }
}
