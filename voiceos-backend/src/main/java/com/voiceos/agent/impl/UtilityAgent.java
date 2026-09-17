package com.voiceos.agent.impl;

import com.voiceos.agent.core.Agent;
import com.voiceos.agent.core.AgentCapability;
import com.voiceos.agent.core.AgentContext;
import com.voiceos.agent.core.AgentRequest;
import com.voiceos.agent.core.AgentResult;
import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.ToolRegistry;
import com.voiceos.tool.core.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic agent for local utilities, including REAL calculator execution.
 */
@Component
public class UtilityAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(UtilityAgent.class);
    private static final Pattern ARITHMETIC = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*([*x×]|\\+|plus|times|multiplied|divided)\\s*(?:by\\s*)?(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE);

    private final ToolRegistry toolRegistry;

    public UtilityAgent(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Override
    public String getName() {
        return "UtilityAgent";
    }

    @Override
    public String getDescription() {
        return "Handles local utility operations such as calculator arithmetic.";
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public Set<AgentCapability> capabilities() {
        return Set.of(AgentCapability.CALCULATOR, AgentCapability.UTILITY);
    }

    @Override
    public boolean canHandle(AgentContext context) {
        String requested = requestedTool(context);
        if (isOwnedTool(requested)) {
            return true;
        }
        String input = context.userInput() != null ? context.userInput().toLowerCase(Locale.ROOT) : "";
        return ARITHMETIC.matcher(input).find()
                || input.contains("calculate")
                || input.contains("calculator")
                || input.contains("multiply")
                || input.contains("multiplied")
                || input.contains("divided by");
    }

    @Override
    public AgentResult execute(AgentRequest request) {
        return execute(request != null ? request.toContext() : AgentContext.of(null, null, null));
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startMs = System.currentTimeMillis();
        if (Thread.currentThread().isInterrupted()) {
            return AgentResult.cancelled(getName(), "Cancelled", System.currentTimeMillis() - startMs);
        }
        String toolName = requestedTool(context);
        if (toolName == null || toolName.isBlank()) {
            toolName = "calculator";
        }
        if (!isOwnedTool(toolName)) {
            return AgentResult.failure(getName(), "UtilityAgent does not authorize tool: " + toolName,
                    System.currentTimeMillis() - startMs);
        }

        Map<String, Object> params = new HashMap<>(toolParams(context));
        if ("calculator".equalsIgnoreCase(toolName) && !params.containsKey("expression")) {
            String expression = extractExpression(context.userInput());
            if (expression != null) {
                params.put("expression", expression);
            }
        }

        log.info("UtilityAgent executing authorized tool={} userId={}", toolName, context.userId());
        ToolResult result = toolRegistry.executeTool(toolName, params);
        long latency = System.currentTimeMillis() - startMs;
        if (result.success()) {
            String text = result.rawOutput() != null ? result.rawOutput() : String.valueOf(result.data());
            return AgentResult.withTools(getName(), text, List.of(result), latency);
        }
        return new AgentResult(
                getName(),
                result.errorMessage(),
                List.of(result),
                false,
                null,
                null,
                false,
                Map.of(),
                latency
        );
    }

    @Override
    public List<Tool> getTools() {
        List<Tool> tools = new ArrayList<>();
        toolRegistry.getTool("calculator").ifPresent(tools::add);
        toolRegistry.getTool("probe_fail").ifPresent(tools::add);
        toolRegistry.getTool("forbidden").ifPresent(tools::add);
        return tools;
    }

    private boolean isOwnedTool(String toolName) {
        if (toolName == null) {
            return false;
        }
        return getTools().stream().anyMatch(tool -> tool.getName().equalsIgnoreCase(toolName));
    }

    private static String requestedTool(AgentContext context) {
        if (context.stateVariables() == null) {
            return null;
        }
        Object value = context.stateVariables().get("requestedTool");
        return value != null ? String.valueOf(value) : null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toolParams(AgentContext context) {
        if (context.stateVariables() == null) {
            return Map.of();
        }
        Object params = context.stateVariables().get("toolParams");
        if (params instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    static String extractExpression(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = input
                .replaceAll("(?i)multiplied by", "*")
                .replaceAll("(?i)times", "*")
                .replaceAll("(?i)divided by", "/")
                .replaceAll("(?i)plus", "+")
                .replaceAll("(?i)minus", "-");
        Matcher matcher = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*([*+/\\-x×])\\s*(\\d+(?:\\.\\d+)?)")
                .matcher(normalized);
        if (matcher.find()) {
            String op = matcher.group(2).replace("x", "*").replace("×", "*");
            return matcher.group(1) + " " + op + " " + matcher.group(3);
        }
        return normalized;
    }
}
