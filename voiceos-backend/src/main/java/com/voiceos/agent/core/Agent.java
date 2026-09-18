package com.voiceos.agent.core;

import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.ToolResult;
import java.util.List;
import java.util.Set;

/**
 * Capability provider in the ActionEngine pipeline.
 *
 * <p>Lower {@link #getPriority()} wins. Agents decide which logical tool is appropriate;
 * PolicyEngine decides whether it is allowed. Agents must not instantiate providers
 * or bypass ToolRegistry.
 *
 * <p>{@link #canHandle(AgentContext)} remains compatibility routing. New selection
 * goes through {@link #canHandle(AgentRequest)}.
 */
public interface Agent {

    String getName();

    String getDescription();

    boolean canHandle(AgentContext context);

    AgentResult execute(AgentContext context);

    List<Tool> getTools();

    /**
     * Lower numeric value = higher priority.
     */
    default int getPriority() {
        return 100;
    }

    default Set<AgentCapability> capabilities() {
        return Set.of();
    }

    default List<Tool> ownedTools() {
        return getTools();
    }

    /**
     * Structured routing. Default delegates to {@link #canHandle(AgentContext)}.
     */
    default boolean canHandle(AgentRequest request) {
        if (request == null) {
            return false;
        }
        if (request.requestedTool() != null && !request.requestedTool().isBlank() && ownsTool(request.requestedTool())) {
            return true;
        }
        String input = request.rawUserInput() != null ? request.rawUserInput() : "";
        AgentContext context = request.toContext();
        return canHandle(new AgentContext(
                context.conversationId(),
                context.userId(),
                input,
                context.history(),
                context.memories(),
                context.stateVariables()
        ));
    }

    default AgentResult execute(AgentRequest request) {
        return execute(request != null ? request.toContext() : AgentContext.of(null, null, null));
    }

    default boolean ownsTool(String toolName) {
        if (toolName == null || ownedTools() == null) {
            return false;
        }
        return ownedTools().stream()
                .anyMatch(tool -> tool != null && tool.getName() != null && tool.getName().equalsIgnoreCase(toolName));
    }

    /**
     * Tools this agent may <em>request</em> for ActionEngine to execute after PolicyEngine.
     * Defaults to {@link #ownsTool(String)}. Fallback agents may allow a smaller server allowlist
     * without owning those tools for routing.
     */
    default boolean allowsTool(String toolName) {
        return ownsTool(toolName);
    }

    /**
     * Continue after ActionEngine executed a requested tool. Default ignores prior results.
     */
    default AgentResult continueWith(AgentRequest request, List<ToolResult> toolResults) {
        return execute(request);
    }
}
