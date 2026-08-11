package com.voiceos.agent.impl;

import com.voiceos.agent.core.Agent;
import com.voiceos.agent.core.AgentContext;
import com.voiceos.agent.core.AgentResult;
import com.voiceos.ai.LLMProvider;
import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.ToolRegistry;
import com.voiceos.tool.core.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Specialized Financial & Budgeting Agent.
 * Calculates costs, estimates trip budgets, and breaks down expense items.
 */
@Component
public class FinanceAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(FinanceAgent.class);
    private final ToolRegistry toolRegistry;
    private final LLMProvider llmProvider;

    public FinanceAgent(ToolRegistry toolRegistry, LLMProvider llmProvider) {
        this.toolRegistry = toolRegistry;
        this.llmProvider = llmProvider;
    }

    @Override
    public String getName() {
        return "FinanceAgent";
    }

    @Override
    public String getDescription() {
        return "Calculates financial estimates, currency conversions, travel budgets, and expense breakdowns.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        String input = context.userInput().toLowerCase();
        return input.contains("budget") || input.contains("cost") || input.contains("expense")
                || input.contains("price") || input.contains("calculate") || input.contains("total");
    }

    @Override
    public int getPriority() {
        return 40;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startMs = System.currentTimeMillis();
        String input = context.userInput();
        log.info("FinanceAgent calculating for: '{}'", input);

        List<ToolResult> tools = new ArrayList<>();
        ToolResult calcResult = toolRegistry.executeTool("calculator", Map.of(
                "expression", "68 + 140 + 50", // mock breakdown: flight + hotel + food/transit
                "operation", "sum"
        ));
        tools.add(calcResult);

        String summary = """
        💰 **Financial Breakdown:**
        - Flight Estimate: $68.00
        - Hotel Estimate: $140.00
        - Local Transit & Meals: $50.00
        - **Estimated Total: $258.00**
        """;

        long latency = System.currentTimeMillis() - startMs;
        return AgentResult.withTools(getName(), summary, tools, latency);
    }

    @Override
    public List<Tool> getTools() {
        return toolRegistry.getAllTools().stream()
                .filter(t -> t.getName().equals("calculator"))
                .toList();
    }
}
