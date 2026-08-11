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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Calendar & Scheduling Agent.
 * Checks availability, finds free time slots, and schedules meetings.
 */
@Component
public class CalendarAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(CalendarAgent.class);
    private final ToolRegistry toolRegistry;
    private final LLMProvider llmProvider;

    public CalendarAgent(ToolRegistry toolRegistry, LLMProvider llmProvider) {
        this.toolRegistry = toolRegistry;
        this.llmProvider = llmProvider;
    }

    @Override
    public String getName() {
        return "CalendarAgent";
    }

    @Override
    public String getDescription() {
        return "Manages schedule, calendar events, meetings, and free slot detection.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        String input = context.userInput().toLowerCase();
        return input.contains("schedule") || input.contains("calendar") || input.contains("meeting")
                || input.contains("free slot") || input.contains("appointment") || input.contains("am i free");
    }

    @Override
    public int getPriority() {
        return 35;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startMs = System.currentTimeMillis();
        String input = context.userInput();
        log.info("CalendarAgent processing: '{}'", input);

        List<ToolResult> tools = new ArrayList<>();
        ToolResult calResult = toolRegistry.executeTool("calendar", Map.of(
                "action", "check_schedule",
                "date", LocalDate.now().toString()
        ));
        tools.add(calResult);

        long latency = System.currentTimeMillis() - startMs;
        return AgentResult.withTools(getName(), calResult.rawOutput(), tools, latency);
    }

    @Override
    public List<Tool> getTools() {
        return toolRegistry.getAllTools().stream()
                .filter(t -> t.getName().equals("calendar"))
                .toList();
    }
}
