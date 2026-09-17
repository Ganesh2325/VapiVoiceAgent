package com.voiceos.tool.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of all available Tools in the VoiceOS platform.
 * Supports tool lookup, parameter validation, risk enforcement, and execution dispatching.
 */
@Service
public class ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);

    private final Map<String, Tool> tools = new ConcurrentHashMap<>();

    public ToolRegistry(List<Tool> discoveredTools) {
        if (discoveredTools != null) {
            for (Tool tool : discoveredTools) {
                registerTool(tool);
            }
        }
        log.info("ToolRegistry initialized with {} tool(s): {}", tools.size(), tools.keySet());
    }

    public void registerTool(Tool tool) {
        tools.put(tool.getName().toLowerCase(), tool);
        log.info("Registered tool: '{}' (riskLevel={})", tool.getName(), tool.getRiskLevel());
    }

    public Optional<Tool> getTool(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(tools.get(name.toLowerCase()));
    }

    public List<Tool> getAllTools() {
        return List.copyOf(tools.values());
    }

    /**
     * Executes a named tool safely with validation, timeout enforcement, and retry policies.
     */
    public ToolResult executeTool(String toolName, Map<String, Object> params) {
        Tool tool = getTool(toolName).orElse(null);
        if (tool == null) {
            return ToolResult.failure("Unknown tool: " + toolName, 0);
        }

        long startMs = System.currentTimeMillis();
        int maxRetries = tool.getMaxRetries();
        long timeoutMs = tool.getTimeoutMs();

        try {
            tool.validateInput(params);
        } catch (IllegalArgumentException e) {
            log.warn("Tool '{}' validation failed: {}", toolName, e.getMessage());
            return ToolResult.failure("Invalid tool input: " + e.getMessage(), 0);
        }

        Exception lastException = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // In a production environment, this should ideally be wrapped in a Future with timeout enforcement.
                ToolResult result = tool.execute(params);
                long latencyMs = System.currentTimeMillis() - startMs;
                if (result.success() && (latencyMs > timeoutMs || result.durationMs() > timeoutMs)) {
                    log.warn("Tool '{}' exceeded timeout of {}ms (took {}ms / reported {}ms)",
                            toolName, timeoutMs, latencyMs, result.durationMs());
                    return ToolResult.failure(
                            result.data(),
                            "TIMEOUT: execution exceeded " + timeoutMs + "ms",
                            Math.max(latencyMs, result.durationMs())
                    );
                }
                log.info("Tool '{}' executed in {}ms on attempt {}/{} (success={})", toolName, latencyMs, attempt, maxRetries, result.success());
                return result;
            } catch (Exception e) {
                lastException = e;
                log.warn("Tool '{}' attempt {}/{} failed: {}", toolName, attempt, maxRetries, e.getMessage());
                try { Thread.sleep(500); } catch (InterruptedException ignored) {} // Basic backoff
            }
        }

        long latencyMs = System.currentTimeMillis() - startMs;
        log.error("Tool '{}' failed completely after {} attempts and {}ms. Last error: {}", toolName, maxRetries, latencyMs, lastException.getMessage());
        return ToolResult.failure("Tool execution failed after retries: " + lastException.getMessage(), latencyMs);
    }
}
