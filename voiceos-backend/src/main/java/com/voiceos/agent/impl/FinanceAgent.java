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
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Financial agent. Does not invent budgets. Honest arithmetic only when an expression is supplied.
 */
@Component
public class FinanceAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(FinanceAgent.class);
    private final ToolRegistry toolRegistry;

    public FinanceAgent(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Override
    public String getName() {
        return "FinanceAgent";
    }

    @Override
    public String getDescription() {
        return "Calculates supplied financial expressions. Does not invent trip budgets.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        String input = context.userInput() != null ? context.userInput().toLowerCase() : "";
        return input.contains("budget") || input.contains("cost") || input.contains("expense")
                || input.contains("price") || input.contains("calculate") || input.contains("total");
    }

    @Override
    public int getPriority() {
        return 40;
    }

    @Override
    public Set<AgentCapability> capabilities() {
        return Set.of(AgentCapability.FINANCE);
    }

    @Override
    public AgentResult execute(AgentRequest request) {
        long startMs = System.currentTimeMillis();
        if (Thread.currentThread().isInterrupted()) {
            return AgentResult.cancelled(getName(), "Cancelled", System.currentTimeMillis() - startMs);
        }
        String expression = request != null ? request.parameterAsString("expression") : null;
        if (expression == null || expression.isBlank()) {
            log.info("FinanceAgent missing expression; refusing to invent a budget");
            return AgentResult.needsInformation(getName(),
                    "Missing required information: expression, amounts. FinanceAgent does not invent budgets.",
                    List.of("expression", "amounts"),
                    System.currentTimeMillis() - startMs);
        }
        if (!ownsTool("calculator")) {
            return AgentResult.notImplemented(getName(),
                    "NOT_IMPLEMENTED: FinanceAgent has no financial provider. It will not invent totals.",
                    System.currentTimeMillis() - startMs);
        }
        ToolResult calcResult = toolRegistry.executeTool("calculator", Map.of("expression", expression));
        long latency = System.currentTimeMillis() - startMs;
        if (calcResult.success()) {
            return AgentResult.withTools(getName(), calcResult.rawOutput(), List.of(calcResult), latency);
        }
        return AgentResult.failure(getName(), calcResult.errorMessage(), latency);
    }

    @Override
    public AgentResult execute(AgentContext context) {
        return execute(AgentRequest.from(context, null));
    }

    @Override
    public List<Tool> getTools() {
        List<Tool> tools = new ArrayList<>();
        toolRegistry.getTool("calculator").ifPresent(tools::add);
        return tools;
    }
}
