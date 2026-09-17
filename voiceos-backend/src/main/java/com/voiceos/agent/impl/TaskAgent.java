package com.voiceos.agent.impl;

import com.voiceos.agent.core.Agent;
import com.voiceos.agent.core.AgentCapability;
import com.voiceos.agent.core.AgentContext;
import com.voiceos.agent.core.AgentResult;
import com.voiceos.ai.LLMProvider;
import com.voiceos.ai.LLMRequest;
import com.voiceos.ai.LLMResponse;
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
 * Task Management Agent.
 * Interacts with the Task database to manage to-do items and reminders.
 */
@Component
public class TaskAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(TaskAgent.class);
    private final ToolRegistry toolRegistry;
    private final LLMProvider llmProvider;

    public TaskAgent(ToolRegistry toolRegistry, LLMProvider llmProvider) {
        this.toolRegistry = toolRegistry;
        this.llmProvider = llmProvider;
    }

    @Override
    public String getName() {
        return "TaskAgent";
    }

    @Override
    public String getDescription() {
        return "Manages user tasks, to-dos, action items, and project checklists.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        String input = context.userInput().toLowerCase();
        return input.contains("task") || input.contains("todo") || input.contains("to-do")
                || input.contains("remind me") || input.contains("checklist");
    }

    @Override
    public int getPriority() {
        return 30;
    }

    @Override
    public Set<AgentCapability> capabilities() {
        return Set.of(AgentCapability.TASKS);
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startMs = System.currentTimeMillis();
        String input = context.userInput();
        log.info("TaskAgent executing: '{}'", input);

        List<ToolResult> toolResults = new ArrayList<>();
        String inputLower = input.toLowerCase();

        String action = "create";
        if (inputLower.contains("list") || inputLower.contains("show") || inputLower.contains("what are my")) {
            action = "list";
        } else if (inputLower.contains("done") || inputLower.contains("complete") || inputLower.contains("finish")) {
            action = "complete";
        }

        // Extract title from input
        String title = input.replaceAll("(?i)(create a task called|create task|add task|remind me to)", "").trim();
        if (title.isBlank()) title = input;

        Map<String, Object> params = new java.util.HashMap<>();
        params.put("action", action);
        params.put("title", title);
        if (context.userId() != null) {
            params.put("userId", context.userId().toString());
        }

        ToolResult result = toolRegistry.executeTool("task_manager", params);
        toolResults.add(result);

        long latency = System.currentTimeMillis() - startMs;
        return AgentResult.withTools(getName(), result.rawOutput(), toolResults, latency);
    }

    @Override
    public List<Tool> getTools() {
        return toolRegistry.getAllTools().stream()
                .filter(t -> t.getName().equals("task_manager"))
                .toList();
    }
}
