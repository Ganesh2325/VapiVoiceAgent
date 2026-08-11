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

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Memory Management Tool for saving and retrieving user preferences and facts.
 * Risk Level: LOW.
 */
@Component
public class MemorySearchTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(MemorySearchTool.class);

    private final MemoryRepository memoryRepository;

    public MemorySearchTool(MemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }

    @Override
    public String getName() {
        return "memory_search";
    }

    @Override
    public String getDescription() {
        return "Searches user's long-term memory for preferences, past facts, and profile settings.";
    }

    @Override
    public ToolRiskLevel getRiskLevel() {
        return ToolRiskLevel.LOW;
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        long startMs = System.currentTimeMillis();
        UUID userId = null;
        if (params.containsKey("userId") && params.get("userId") != null) {
            userId = UUID.fromString(params.get("userId").toString());
        }

        List<Memory> memories;
        if (userId != null) {
            memories = memoryRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        } else {
            memories = memoryRepository.findAll();
        }

        List<Map<String, String>> memoryList = memories.stream()
                .map(m -> Map.of(
                        "key", m.getKey() != null ? m.getKey() : "general",
                        "content", m.getContent(),
                        "type", m.getMemoryType().name()
                ))
                .toList();

        long latency = System.currentTimeMillis() - startMs;
        return ToolResult.success(
                Map.of("memories", memoryList, "count", memories.size()),
                "Retrieved " + memories.size() + " memory entry/entries.",
                latency
        );
    }
}
