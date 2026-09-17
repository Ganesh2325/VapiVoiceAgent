package com.voiceos.tool.impl;

import com.voiceos.domain.repository.MemoryRepository;
import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.ToolResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Component
public class MemoryDeleteTool implements Tool {

    private final MemoryRepository memoryRepository;

    public MemoryDeleteTool(MemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }

    @Override
    public String getName() {
        return "memory_delete";
    }

    @Override
    public String getDescription() {
        return "Deletes a specific memory or preference for the user based on content keyword.";
    }

    @Override
    public ToolRiskLevel getRiskLevel() {
        return ToolRiskLevel.MEDIUM; // Users can delete their own memories, low risk but modifies state
    }

    @Override
    @Transactional
    public ToolResult execute(Map<String, Object> params) {
        try {
            String userIdStr = (String) params.get("userId");
            if (userIdStr == null) {
                return ToolResult.failure("userId is required", 0);
            }
            UUID userId = UUID.fromString(userIdStr);
            String keyword = (String) params.get("keyword");
            if (keyword == null || keyword.isBlank()) {
                return ToolResult.failure("keyword is required to find the memory to delete", 0);
            }

            // A real implementation might use an LLM or vector search to find the exact memory to delete.
            // For MVP, we will delete all memories containing the keyword for this user.
            var memories = memoryRepository.findByUserIdOrderByUpdatedAtDesc(userId);
            boolean deleted = false;
            for (var memory : memories) {
                if (memory.getContent().toLowerCase().contains(keyword.toLowerCase())) {
                    memoryRepository.delete(memory);
                    deleted = true;
                }
            }

            if (deleted) {
                return ToolResult.success(
                        Map.of("deleted", true, "message", "Memory deleted"),
                        "Successfully forgot the requested memory.",
                        0);
            } else {
                return ToolResult.success(
                        Map.of("deleted", false, "message", "No matching memory found"),
                        "I couldn't find a memory matching that description to forget.",
                        0);
            }

        } catch (Exception e) {
            return ToolResult.failure("Failed to delete memory: " + e.getMessage(), 0);
        }
    }
}
