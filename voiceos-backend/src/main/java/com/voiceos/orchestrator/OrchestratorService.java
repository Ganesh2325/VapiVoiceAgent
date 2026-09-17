package com.voiceos.orchestrator;

import com.voiceos.action.engine.ActionEngine;
import com.voiceos.action.model.ActionCommand;
import com.voiceos.action.model.ActionExecutionResult;
import com.voiceos.agent.core.Agent;
import com.voiceos.agent.core.AgentContext;
import com.voiceos.agent.core.AgentRegistry;
import com.voiceos.agent.core.AgentResult;
import com.voiceos.ai.LLMProvider;
import com.voiceos.ai.LLMRequest;
import com.voiceos.ai.LLMResponse;
import com.voiceos.domain.entity.*;
import com.voiceos.domain.repository.*;
import com.voiceos.exception.VoiceOsException;
import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.ToolRegistry;
import com.voiceos.tool.core.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * VoiceOS Orchestrator Service — The Central Brain of the Platform.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Evaluates user intent and decomposes complex tasks into execution plans</li>
 *   <li>Routes sub-goals to specialized domain agents</li>
 *   <li>Manages state handoffs and memory retrieval across agent steps</li>
 *   <li>Enforces Human-in-the-Loop approvals for HIGH and CRITICAL risk actions</li>
 *   <li>Publishes real-time execution events to WebSocket subscribers</li>
 *   <li>Persists execution history, messages, and quality evaluations in PostgreSQL</li>
 * </ul>
 */
@Service
public class OrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorService.class);

    private final AgentRegistry agentRegistry;
    private final ToolRegistry toolRegistry;
    private final LLMProvider llmProvider;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final AgentExecutionRepository agentExecutionRepository;
    private final ToolExecutionRepository toolExecutionRepository;
    private final AgentPlanRepository agentPlanRepository;
    private final ApprovalRepository approvalRepository;
    private final MemoryRepository memoryRepository;
    private final EvaluationRepository evaluationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ActionEngine actionEngine;

    public OrchestratorService(
            AgentRegistry agentRegistry,
            ToolRegistry toolRegistry,
            LLMProvider llmProvider,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            AgentExecutionRepository agentExecutionRepository,
            ToolExecutionRepository toolExecutionRepository,
            AgentPlanRepository agentPlanRepository,
            ApprovalRepository approvalRepository,
            MemoryRepository memoryRepository,
            EvaluationRepository evaluationRepository,
            SimpMessagingTemplate messagingTemplate,
            ActionEngine actionEngine
    ) {
        this.agentRegistry = agentRegistry;
        this.toolRegistry = toolRegistry;
        this.llmProvider = llmProvider;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.agentExecutionRepository = agentExecutionRepository;
        this.toolExecutionRepository = toolExecutionRepository;
        this.agentPlanRepository = agentPlanRepository;
        this.approvalRepository = approvalRepository;
        this.memoryRepository = memoryRepository;
        this.evaluationRepository = evaluationRepository;
        this.messagingTemplate = messagingTemplate;
        this.actionEngine = actionEngine;
    }

    /**
     * Executes instruction with default or existing user.
     */
    @Transactional
    public OrchestrationResult execute(UUID conversationId, String userInput) {
        return processUserRequest(conversationId, null, userInput);
    }

    /**
     * Main entry point for processing a user request.
     *
     * @param conversationId the conversation UUID
     * @param userId         the requesting user UUID
     * @param userInput      the user text or transcribed voice command
     * @return final orchestration result
     */
    @Transactional
    public OrchestrationResult processUserRequest(UUID conversationId, UUID userId, String userInput) {
        long startMs = System.currentTimeMillis();
        log.info("Orchestrator delegating to ActionEngine conversation={} user={}", conversationId, userId);
        ActionExecutionResult result = actionEngine.execute(new ActionCommand(
                userId,
                conversationId,
                null,
                null,
                null,
                null,
                "HTTP",
                userInput,
                null,
                Map.of()
        ));
        long latency = result.totalLatencyMs() > 0 ? result.totalLatencyMs() : System.currentTimeMillis() - startMs;
        return new OrchestrationResult(
                result.conversationId() != null ? result.conversationId() : conversationId,
                result.responseText(),
                result.activeAgent(),
                result.awaitingApproval(),
                result.toolsExecuted(),
                latency
        );
    }

    // ─── Multi-Step Planning & Execution ─────────────────────────────────────

    private AgentResult executeMultiStepPlan(Conversation conversation, AgentContext context) {
        log.info("Generating multi-step plan for: '{}'", context.userInput());

        // 1. Build Plan Steps
        List<AgentPlan.PlanStep> steps = generatePlanSteps(context.userInput());

        // 2. Persist AgentPlan
        AgentPlan plan = new AgentPlan(conversation, conversation.getUser(), context.userInput(), steps);
        plan.setStatus(AgentPlan.PlanStatus.EXECUTING);
        plan = agentPlanRepository.save(plan);

        broadcastEvent(conversation.getId(), "PLAN_CREATED", Map.of("stepsCount", steps.size(), "goal", plan.getGoal()));

        List<ToolResult> allToolResults = new ArrayList<>();
        StringBuilder synthesizedOutput = new StringBuilder();

        // 3. Execute steps sequentially
        for (int i = 0; i < steps.size(); i++) {
            AgentPlan.PlanStep step = steps.get(i);
            plan.setCurrentStepIndex(i);
            log.info("Executing plan step {}: agent='{}', action='{}'", i, step.agentName(), step.action());

            broadcastEvent(conversation.getId(), "PLAN_STEP_STARTED", Map.of(
                    "stepIndex", i, "agent", step.agentName(), "action", step.action()
            ));

            Optional<Agent> agentOpt = agentRegistry.getAgent(step.agentName());
            if (agentOpt.isPresent()) {
                Agent agent = agentOpt.get();
                conversation.setActiveAgentName(agent.getName());

                AgentExecution exec = new AgentExecution(conversation, conversation.getUser(), agent.getName(), step.description());
                exec.markStarted();
                exec = agentExecutionRepository.save(exec);

                AgentContext stepContext = new AgentContext(
                        context.conversationId(),
                        context.userId(),
                        step.description(),
                        context.history(),
                        context.memories(),
                        context.stateVariables()
                );

                AgentResult result = agent.execute(stepContext);

                if (result.requiresApproval()) {
                    handleApprovalPause(conversation, exec, result);
                    plan.setStatus(AgentPlan.PlanStatus.EXECUTING);
                    agentPlanRepository.save(plan);
                    return result;
                }

                exec.markCompleted(result.responseText());
                agentExecutionRepository.save(exec);

                allToolResults.addAll(result.toolExecutions());
                synthesizedOutput.append(result.responseText()).append("\n\n");
            }
        }

        plan.setStatus(AgentPlan.PlanStatus.COMPLETED);
        agentPlanRepository.save(plan);

        return AgentResult.withTools("Orchestrator", synthesizedOutput.toString().trim(), allToolResults, 0);
    }

    private List<AgentPlan.PlanStep> generatePlanSteps(String input) {
        log.info("Requesting plan from PlannerAgent for input: {}", input);
        
        Optional<Agent> plannerOpt = agentRegistry.getAgent("PlannerAgent");
        if (plannerOpt.isEmpty()) {
            return List.of(new AgentPlan.PlanStep(0, "ConversationAgent", "respond", "Process and answer user request", "PENDING", null));
        }
        
        AgentResult result = plannerOpt.get().execute(new AgentContext(null, null, input, List.of(), List.of(), Map.of()));
        if (!result.success() || !result.responseText().startsWith("[")) {
            return List.of(new AgentPlan.PlanStep(0, "ConversationAgent", "respond", "Process and answer user request", "PENDING", null));
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, String>> parsedSteps = mapper.readValue(result.responseText(), java.util.List.class);
            List<AgentPlan.PlanStep> steps = new ArrayList<>();
            for (int i = 0; i < parsedSteps.size(); i++) {
                Map<String, String> s = parsedSteps.get(i);
                steps.add(new AgentPlan.PlanStep(
                        i, 
                        s.getOrDefault("agentName", "ConversationAgent"), 
                        s.getOrDefault("action", "execute"), 
                        s.getOrDefault("description", "Execute step"), 
                        "PENDING", 
                        null
                ));
            }
            return steps;
        } catch (Exception e) {
            log.error("Failed to parse PlannerAgent output: {}", e.getMessage());
            return List.of(new AgentPlan.PlanStep(0, "ConversationAgent", "respond", "Process and answer user request", "PENDING", null));
        }
    }

    private boolean isMultiStepGoal(String input) {
        String lower = input.toLowerCase();
        return (lower.contains("trip") && (lower.contains("plan") || lower.contains("bangalore")))
                || (lower.contains("research") && lower.contains("email"))
                || (lower.contains("itinerary") && lower.contains("budget"));
    }

    // ─── Human-in-the-Loop Approval Handling ─────────────────────────────────

    private void handleApprovalPause(Conversation conversation, AgentExecution exec, AgentResult result) {
        AgentResult.ApprovalRequestData data = result.approvalAction();
        log.warn("Execution paused — human approval required for action: '{}'", data.actionType());

        Approval approval = new Approval(
                conversation.getUser(),
                conversation,
                exec,
                data.actionType(),
                data.actionDescription(),
                ToolExecution.RiskLevel.valueOf(data.riskLevel()),
                data.payload(),
                Instant.now().plus(24, ChronoUnit.HOURS)
        );
        approval = approvalRepository.save(approval);

        broadcastEvent(conversation.getId(), "APPROVAL_REQUIRED", Map.of(
                "approvalId", approval.getId().toString(),
                "actionType", approval.getActionType(),
                "description", approval.getActionDescription(),
                "riskLevel", approval.getRiskLevel().name()
        ));
    }

    // ─── WebSocket Event Broadcasting ────────────────────────────────────────

    private void broadcastEvent(UUID conversationId, String eventType, Map<String, Object> payload) {
        try {
            Map<String, Object> message = new HashMap<>(payload);
            message.put("eventType", eventType);
            message.put("conversationId", conversationId.toString());
            message.put("timestamp", Instant.now().toString());

            messagingTemplate.convertAndSend("/topic/executions/" + conversationId, message);
            log.debug("Broadcast event '{}' to /topic/executions/{}", eventType, conversationId);
        } catch (Exception e) {
            log.warn("Failed to broadcast WebSocket event: {}", e.getMessage());
        }
    }

    public record OrchestrationResult(
            UUID conversationId,
            String responseText,
            String activeAgent,
            boolean awaitingApproval,
            List<ToolResult> toolsExecuted,
            long totalLatencyMs
    ) {}
}
