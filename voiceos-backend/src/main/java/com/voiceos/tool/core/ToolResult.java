package com.voiceos.tool.core;

import java.util.Map;

/**
 * Structured result returned by a Tool execution.
 *
 * @param success      whether the tool executed successfully
 * @param data         structured output data map
 * @param rawOutput    human-readable text summary of the output
 * @param errorMessage error message if execution failed
 * @param durationMs   execution duration in milliseconds
 */
public record ToolResult(
        boolean success,
        Map<String, Object> data,
        String rawOutput,
        String errorMessage,
        long durationMs
) {

    public static ToolResult success(Map<String, Object> data, String rawOutput, long durationMs) {
        return new ToolResult(true, data != null ? data : Map.of(), rawOutput, null, durationMs);
    }

    public static ToolResult success(String rawOutput, long durationMs) {
        return new ToolResult(true, Map.of("result", rawOutput), rawOutput, null, durationMs);
    }

    public static ToolResult failure(String errorMessage, long durationMs) {
        return new ToolResult(false, Map.of(), null, errorMessage, durationMs);
    }
}
