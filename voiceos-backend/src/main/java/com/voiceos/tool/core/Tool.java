package com.voiceos.tool.core;

import com.voiceos.provider.ProviderMode;

import java.util.Map;

/**
 * Core Tool interface for VoiceOS.
 *
 * <p>Tools encapsulate deterministic capabilities (e.g. database lookups,
 * web searches, calendar checks, sending emails) that agents can invoke.
 *
 * <p>All tools must:
 * <ul>
 *   <li>Declare a strict risk level (LOW, MEDIUM, HIGH, CRITICAL)</li>
 *   <li>Validate all input parameters strictly (never trust LLM outputs blindly)</li>
 *   <li>Implement timeout and retry policies</li>
 * </ul>
 */
public interface Tool {

    /**
     * @return the unique name of the tool (e.g. "search_web", "create_task")
     */
    String getName();

    /**
     * @return description of what the tool does and when to call it
     */
    String getDescription();

    /**
     * @return risk classification determining if human approval is required
     */
    ToolRiskLevel getRiskLevel();

    /**
     * Executes the tool with the given validated parameters.
     *
     * @param params map of parameter names to values
     * @return tool execution result
     */
    ToolResult execute(Map<String, Object> params);

    /**
     * Validates input parameters before execution.
     *
     * @param params parameters provided by LLM or orchestrator
     * @throws IllegalArgumentException if validation fails
     */
    default void validateInput(Map<String, Object> params) {
        if (params == null) {
            throw new IllegalArgumentException("Tool parameters map cannot be null");
        }
    }

    /**
     * @return execution timeout in milliseconds
     */
    default long getTimeoutMs() {
        return 10000;
    }

    /**
     * @return max retry attempts on transient failure
     */
    default int getMaxRetries() {
        return 2;
    }

    default String getProviderName() {
        return "none";
    }

    default ProviderMode getProviderMode() {
        return ProviderMode.REAL;
    }

    /**
     * Risk levels governing human-in-the-loop approval:
     * <ul>
     *   <li>LOW: Read-only, safe (e.g. search, calculate) → execute immediately</li>
     *   <li>MEDIUM: Low-impact state modification (e.g. create task, add note) → execute immediately</li>
     *   <li>HIGH: High-impact modification (e.g. send email, update external record) → requires approval</li>
     *   <li>CRITICAL: Financial, destructive operations (e.g. delete data, transfer funds) → requires approval</li>
     * </ul>
     */
    enum ToolRiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL;

        public boolean requiresHumanApproval() {
            return this == HIGH || this == CRITICAL;
        }
    }
}
