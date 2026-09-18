package com.voiceos.action.engine;

import com.voiceos.action.audit.ActionTimelineDto;
import com.voiceos.action.audit.ActionTraceService;
import com.voiceos.action.audit.SensitiveDataRedactor;
import com.voiceos.action.model.Action;
import com.voiceos.action.model.ActionCommand;
import com.voiceos.action.model.ActionExecutionResult;
import com.voiceos.action.model.ActionStatus;
import com.voiceos.action.model.ActionStep;
import com.voiceos.action.model.ActionStepType;
import com.voiceos.action.model.AuditActorType;
import com.voiceos.action.model.AuditEventType;
import com.voiceos.action.store.ActionStore;
import com.voiceos.agent.core.Agent;
import com.voiceos.agent.core.AgentOutcome;
import com.voiceos.agent.core.AgentRequest;
import com.voiceos.agent.core.AgentResult;
import com.voiceos.agent.decision.ToolArgumentSanitizer;
import com.voiceos.agent.core.AgentSelector;
import com.voiceos.config.VoiceOsProperties;
import com.voiceos.domain.entity.Conversation;
import com.voiceos.domain.repository.ConversationRepository;
import com.voiceos.domain.repository.UserRepository;
import com.voiceos.security.RequestCorrelationFilter;
import com.voiceos.security.VoiceOsRequestContext;
import com.voiceos.service.PolicyDecision;
import com.voiceos.service.PolicyEngine;
import com.voiceos.service.PolicyRequest;
import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.Tool.ToolRiskLevel;
import com.voiceos.tool.core.ToolRegistry;
import com.voiceos.tool.core.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Canonical coordinator for VoiceOS business execution.
 *
 * <p>Path: validate → persist Action → PolicyEngine → AgentSelector → Agent →
 * ToolRegistry → Provider → ResultVerifier → ActionResult.
 *
 * <p>{@link ActionTraceService} observes this path. It never decides execution.
 */
@Service
public class ActionEngine {

    public static final String MDC_ACTION_ID = "actionId";
    private static final Logger log = LoggerFactory.getLogger(ActionEngine.class);

    private final ActionStore actionStore;
    private final PolicyEngine policyEngine;
    private final AgentSelector agentSelector;
    private final ToolRegistry toolRegistry;
    private final ResultVerifier resultVerifier;
    private final VoiceOsProperties properties;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final ActionPlanner actionPlanner;
    private final ActionExecutor actionExecutor;
    private final ActionTraceService actionTraceService;

    @Autowired
    public ActionEngine(
            ActionStore actionStore,
            PolicyEngine policyEngine,
            AgentSelector agentSelector,
            ToolRegistry toolRegistry,
            ResultVerifier resultVerifier,
            VoiceOsProperties properties,
            @Autowired(required = false) ConversationRepository conversationRepository,
            @Autowired(required = false) UserRepository userRepository,
            @Autowired(required = false) ActionPlanner actionPlanner,
            @Autowired(required = false) ActionExecutor actionExecutor,
            @Autowired(required = false) ActionTraceService actionTraceService
    ) {
        this.actionStore = actionStore;
        this.policyEngine = policyEngine;
        this.agentSelector = agentSelector;
        this.toolRegistry = toolRegistry;
        this.resultVerifier = resultVerifier;
        this.properties = properties;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.actionPlanner = actionPlanner;
        this.actionExecutor = actionExecutor;
        this.actionTraceService = actionTraceService;
    }

    public ActionExecutionResult execute(ActionCommand command) {
        long startMs = System.currentTimeMillis();
        if (command.idempotencyKey() != null && !command.idempotencyKey().isBlank()) {
            Optional<Action> existing = actionStore.findByIdempotencyKey(command.idempotencyKey());
            if (existing.isPresent()) {
                Action prior = existing.get();
                log.info("Action idempotent replay actionId={} key={} status={}",
                        prior.getId(), command.idempotencyKey(), prior.getStatus());
                return ActionExecutionResult.from(prior, List.of(), duration(startMs));
            }
        }

        Action action = new Action(command.userId(), command.requestedOperation());
        action.setStatus(ActionStatus.RECEIVED);
        action.setCallId(command.callId());
        action.setRequestId(firstNonBlank(command.requestId(), VoiceOsRequestContext.currentRequestId()));
        action.setEventId(command.eventId());
        action.setIdempotencyKey(command.idempotencyKey());
        action.setActionType(firstNonBlank(command.requestedTool(), command.requestedOperation(), "unknown"));
        action.setPayload(SensitiveDataRedactor.redact(command.params() != null ? command.params() : Map.of()));
        persist(action);

        MDC.put(MDC_ACTION_ID, action.getId().toString());
        try {
            ActionStep received = beginStep(action, ActionStepType.RECEIVED);
            emit(action, received, AuditEventType.ACTION_CREATED, actorFor(command),
                    Map.of("message", "Action received", "source", command.source() != null ? command.source() : "UNKNOWN"));
            succeedStep(received, Map.of("actionType", String.valueOf(action.getActionType())));
            return runPipeline(action, command, startMs);
        } finally {
            MDC.remove(MDC_ACTION_ID);
        }
    }

    private ActionExecutionResult runPipeline(Action action, ActionCommand command, long startMs) {
        ActionStep validating = beginStep(action, ActionStepType.VALIDATING);
        if (command.requestedOperation() == null && command.requestedTool() == null) {
            failStep(validating, "UNKNOWN_ACTION", "Unknown action");
            return finish(action, ActionStatus.REJECTED, "Unknown action", startMs, List.of(), AuditEventType.ACTION_REJECTED);
        }
        action.setStatus(ActionStatus.VALIDATED);
        persist(action);
        succeedStep(validating, Map.of());
        emit(action, validating, AuditEventType.ACTION_VALIDATED, AuditActorType.SYSTEM,
                Map.of("message", "Action validated"));

        ActionStep authorizing = beginStep(action, ActionStepType.AUTHORIZING);
        if (command.userId() == null) {
            failStep(authorizing, "UNBOUND_SESSION", "Voice session is not bound to a VoiceOS user");
            emit(action, authorizing, AuditEventType.IDENTITY_UNRESOLVED, AuditActorType.VAPI,
                    Map.of("message", "Voice session is not bound to a VoiceOS user"));
            return finish(action, ActionStatus.REJECTED, "Voice session is not bound to a VoiceOS user",
                    startMs, List.of(), AuditEventType.ACTION_REJECTED);
        }

        UUID conversationId = resolveConversation(command);
        action.setUserId(command.userId());
        action.setConversationId(conversationId);
        action.setStatus(ActionStatus.AUTHORIZED);
        persist(action);
        applyRequestContext(action);
        succeedStep(authorizing, Map.of("userId", command.userId().toString()));
        emit(action, authorizing, AuditEventType.IDENTITY_RESOLVED, AuditActorType.USER,
                Map.of("message", "Identity resolved"));

        AgentRequest agentRequest = AgentRequest.from(command, action.getId());

        ActionStep planning = beginStep(action, ActionStepType.PLANNING);
        Optional<Agent> selected = agentSelector.select(agentRequest);
        if (selected.isEmpty()) {
            failStep(planning, "NO_AGENT", "No agent available for this action");
            return finish(action, ActionStatus.FAILED, "No agent available for this action",
                    startMs, List.of(), AuditEventType.ACTION_FAILED);
        }
        Agent agent = selected.get();
        action.setTargetAgent(agent.getName());

        String toolName = resolveToolName(agent, command);
        action.setToolName(toolName);
        Optional<Tool> tool = toolName != null ? toolRegistry.getTool(toolName) : Optional.empty();
        if (tool.isPresent()) {
            action.setTargetProvider(tool.get().getProviderName());
            action.setProviderMode(tool.get().getProviderMode().name());
        }
        action.setStatus(ActionStatus.PLANNED);
        persist(action);
        if (planning != null) {
            planning.setAgentName(agent.getName());
            planning.setToolName(toolName);
            planning.setProviderName(action.getTargetProvider());
            planning.setProviderMode(action.getProviderMode());
        }
        succeedStep(planning, Map.of("agent", agent.getName(), "tool", String.valueOf(toolName)));
        emit(action, planning, AuditEventType.AGENT_SELECTED, AuditActorType.AGENT,
                Map.of("message", "Agent selected", "agent", agent.getName()));
        if (toolName != null) {
            emit(action, planning, AuditEventType.TOOL_SELECTED, AuditActorType.TOOL,
                    Map.of("message", "Tool selected", "tool", toolName));
        }

        ActionStep policyStep = beginStep(action, ActionStepType.POLICY_CHECK);
        ToolRiskLevel risk = tool.map(Tool::getRiskLevel)
                .orElseGet(() -> PolicyEngine.inferRisk(toolName, action.getActionType()));
        PolicyDecision decision = policyEngine.evaluate(new PolicyRequest(
                command.userId(),
                action.getActionType(),
                toolName,
                risk,
                action.getTargetProvider(),
                command.params()
        ));
        emit(action, policyStep, AuditEventType.POLICY_EVALUATED, AuditActorType.SYSTEM, Map.of(
                "message", "Policy evaluated",
                "outcome", decision.outcome().name(),
                "reasonCode", decision.reasonCode() != null ? decision.reasonCode() : ""
        ));

        if (decision.isDeny()) {
            log.info("Policy denied actionId={} tool={} reasonCode={}",
                    action.getId(), toolName, decision.reasonCode());
            failStep(policyStep, decision.reasonCode(), decision.reason());
            emit(action, policyStep, AuditEventType.POLICY_DENIED, AuditActorType.SYSTEM,
                    Map.of("message", decision.reason() != null ? decision.reason() : "Denied",
                            "reasonCode", decision.reasonCode() != null ? decision.reasonCode() : "DENY"));
            return finish(action, ActionStatus.REJECTED, decision.reason(), startMs, List.of(), AuditEventType.ACTION_REJECTED);
        }
        if (decision.isRequireApproval()) {
            succeedStep(policyStep, Map.of("outcome", "REQUIRE_APPROVAL"));
            ActionStep wait = beginStep(action, ActionStepType.APPROVAL_WAIT);
            waitUserStep(wait, decision.reason());
            emit(action, wait, AuditEventType.APPROVAL_REQUIRED, AuditActorType.SYSTEM,
                    Map.of("message", decision.reason() != null ? decision.reason() : "Approval required"));
            action.setErrorMessage(decision.reason());
            action.setStatus(ActionStatus.REQUIRES_APPROVAL);
            persist(action);
            logPipeline(action, startMs);
            return ActionExecutionResult.from(action, List.of(), duration(startMs));
        }
        succeedStep(policyStep, Map.of("outcome", "ALLOW"));

        if (toolName != null && tool.isEmpty()) {
            return finish(action, ActionStatus.FAILED, "Unknown tool: " + toolName,
                    startMs, List.of(), AuditEventType.ACTION_FAILED);
        }
        if (toolName != null && !agentOwnsTool(agent, toolName)) {
            return finish(action, ActionStatus.FAILED, "Agent does not authorize tool: " + toolName,
                    startMs, List.of(), AuditEventType.ACTION_FAILED);
        }

        action.setStatus(ActionStatus.EXECUTING);
        action.setStartedAt(Instant.now());
        persist(action);

        emit(action, planning, AuditEventType.AGENT_STARTED, AuditActorType.AGENT,
                Map.of("message", "Agent started", "agent", agent.getName()));

        AgentResult agentResult;
        try {
            if (Thread.currentThread().isInterrupted()) {
                emit(action, planning, AuditEventType.AGENT_FAILED, AuditActorType.AGENT,
                        Map.of("message", "Agent cancelled"));
                return finish(action, ActionStatus.CANCELLED, "Cancelled", startMs, List.of(), AuditEventType.ACTION_CANCELLED);
            }
            agentResult = agent.execute(agentRequest);
        } catch (RuntimeException e) {
            log.error("Agent execution failed actionId={} agent={}", action.getId(), agent.getName());
            emit(action, planning, AuditEventType.AGENT_FAILED, AuditActorType.AGENT,
                    Map.of("message", "Agent execution failed"));
            return finish(action, ActionStatus.FAILED, "Agent execution failed: " + e.getClass().getSimpleName(),
                    startMs, List.of(), AuditEventType.ACTION_FAILED);
        }

        AgentOutcome outcome = agentResult.outcome();
        recordAgentOutcome(action, agentResult);
        applyLlmMetadata(action, agentResult);
        if (agentResult.selectedTool() != null && !agentResult.selectedTool().isBlank()) {
            String proposed = agentResult.selectedTool();
            boolean owned = agentOwnsTool(agent, proposed);
            boolean sameAsPolicyTool = toolName == null || toolName.equalsIgnoreCase(proposed);
            if (owned && sameAsPolicyTool) {
                action.setToolName(proposed);
            }
        }

        AgentResult[] current = { agentResult };
        List<ToolResult> serverExecuted = new ArrayList<>();
        ActionExecutionResult toolLoopTerminal = executeApprovedModelTools(
                action, command, agent, agentRequest, current, planning, startMs, serverExecuted);
        if (toolLoopTerminal != null) {
            return toolLoopTerminal;
        }
        agentResult = current[0];
        outcome = agentResult.outcome();
        recordAgentOutcome(action, agentResult);
        applyLlmMetadata(action, agentResult);

        if (outcome == AgentOutcome.NEEDS_INFORMATION || outcome == AgentOutcome.WAITING_USER) {
            emit(action, planning, AuditEventType.AGENT_SUCCEEDED, AuditActorType.AGENT,
                    Map.of("message", "Agent needs information"));
            ActionStep wait = beginStep(action, ActionStepType.APPROVAL_WAIT);
            waitUserStep(wait, agentResult.responseText());
            action.setErrorMessage(agentResult.responseText());
            action.setResult(null);
            action.setStatus(ActionStatus.WAITING_FOR_USER);
            persist(action);
            logPipeline(action, startMs);
            return ActionExecutionResult.from(action, List.of(), duration(startMs));
        }
        if (outcome == AgentOutcome.NEEDS_USER_CONFIRMATION || agentResult.requiresApproval()) {
            emit(action, planning, AuditEventType.AGENT_SUCCEEDED, AuditActorType.AGENT,
                    Map.of("message", "Agent requires user confirmation"));
            ActionStep wait = beginStep(action, ActionStepType.APPROVAL_WAIT);
            waitUserStep(wait, agentResult.responseText());
            emit(action, wait, AuditEventType.APPROVAL_REQUIRED, AuditActorType.SYSTEM,
                    Map.of("message", agentResult.responseText() != null ? agentResult.responseText() : "Confirmation required"));
            action.setErrorMessage(agentResult.responseText());
            action.setStatus(ActionStatus.REQUIRES_APPROVAL);
            persist(action);
            logPipeline(action, startMs);
            return ActionExecutionResult.from(action, List.of(), duration(startMs));
        }
        if (outcome == AgentOutcome.CANCELLED) {
            emit(action, planning, AuditEventType.AGENT_FAILED, AuditActorType.AGENT,
                    Map.of("message", "Agent cancelled"));
            return finish(action, ActionStatus.CANCELLED,
                    firstNonBlank(agentResult.responseText(), "Cancelled"),
                    startMs, List.of(), AuditEventType.ACTION_CANCELLED);
        }
        if (outcome == AgentOutcome.REJECTED && (agentResult.toolExecutions() == null || agentResult.toolExecutions().isEmpty())) {
            emit(action, planning, AuditEventType.AGENT_FAILED, AuditActorType.AGENT,
                    Map.of("message", "Agent rejected the request"));
            return finish(action, ActionStatus.REJECTED,
                    firstNonBlank(agentResult.responseText(), "Rejected"),
                    startMs, List.of(), AuditEventType.ACTION_REJECTED);
        }

        List<ToolResult> tools = !serverExecuted.isEmpty()
                ? List.copyOf(serverExecuted)
                : (agentResult.toolExecutions() != null ? agentResult.toolExecutions() : List.of());
        boolean noTools = tools.isEmpty();
        if (noTools && (outcome == AgentOutcome.FAILED
                || outcome == AgentOutcome.NOT_IMPLEMENTED
                || outcome == AgentOutcome.UNAVAILABLE
                || !agentResult.success())) {
            emit(action, planning, AuditEventType.AGENT_FAILED, AuditActorType.AGENT,
                    Map.of("message", firstNonBlank(agentResult.responseText(), "Agent failed")));
            return finish(action, ActionStatus.FAILED,
                    firstNonBlank(agentResult.responseText(), "Agent execution failed"),
                    startMs, List.of(), AuditEventType.ACTION_FAILED);
        }

        ToolResult primary = noTools ? null : tools.get(tools.size() - 1);
        applyProviderMetadata(action, primary);
        if (primary != null && action.getToolName() == null) {
            action.setToolName(toolName);
        }

        ActionStep execution = null;
        boolean toolFailed = primary != null && !primary.success();
        boolean modelToolAlreadyRun = !serverExecuted.isEmpty();
        if (!tools.isEmpty() && !modelToolAlreadyRun) {
            execution = beginStep(action, ActionStepType.TOOL_EXECUTION);
            emit(action, execution, AuditEventType.TOOL_STARTED, AuditActorType.TOOL,
                    Map.of("message", "Tool started", "tool", String.valueOf(action.getToolName() != null ? action.getToolName() : toolName)));
            if (action.getTargetProvider() != null) {
                emit(action, execution, AuditEventType.PROVIDER_STARTED, AuditActorType.PROVIDER, Map.of(
                        "message", "Provider started",
                        "provider", action.getTargetProvider(),
                        "providerMode", action.getProviderMode() != null ? action.getProviderMode() : "unavailable"
                ));
            }
            if (toolFailed) {
                action.setErrorMessage(primary.errorMessage());
                action.setResult(null);
                failStep(execution, "TOOL_FAILED", primary.errorMessage());
                emit(action, execution, AuditEventType.PROVIDER_FAILED, AuditActorType.PROVIDER,
                        Map.of("message", primary.errorMessage() != null ? primary.errorMessage() : "Provider failed"));
                emit(action, execution, AuditEventType.TOOL_FAILED, AuditActorType.TOOL,
                        Map.of("message", primary.errorMessage() != null ? primary.errorMessage() : "Tool failed"));
            } else {
                action.setResult(agentResult.responseText());
                succeedStep(execution, Map.of("result", agentResult.responseText() != null ? agentResult.responseText() : ""));
                if (action.getTargetProvider() != null) {
                    emit(action, execution, AuditEventType.PROVIDER_SUCCEEDED, AuditActorType.PROVIDER, Map.of(
                            "message", "Provider succeeded",
                            "providerMode", action.getProviderMode() != null ? action.getProviderMode() : "unavailable"
                    ));
                }
                emit(action, execution, AuditEventType.TOOL_SUCCEEDED, AuditActorType.TOOL,
                        Map.of("message", "Tool succeeded", "tool", String.valueOf(action.getToolName())));
            }
        } else if (agentResult.responseText() != null) {
            action.setResult(agentResult.responseText());
        }

        if (toolFailed || !agentResult.success()) {
            emit(action, execution != null ? execution : planning, AuditEventType.AGENT_FAILED, AuditActorType.AGENT,
                    Map.of("message", "Agent failed", "agent", agent.getName()));
        } else {
            emit(action, execution != null ? execution : planning, AuditEventType.AGENT_SUCCEEDED, AuditActorType.AGENT,
                    Map.of("message", "Agent succeeded", "agent", agent.getName()));
        }

        long timeoutMs = properties != null && properties.agents() != null
                ? properties.agents().executionTimeoutMs()
                : 30_000L;
        if (primary != null && primary.durationMs() > timeoutMs) {
            return finish(action, ActionStatus.FAILED, "TIMEOUT: execution exceeded timeout of " + timeoutMs + "ms",
                    startMs, tools, AuditEventType.ACTION_FAILED);
        }

        action.setStatus(ActionStatus.VERIFYING);
        persist(action);

        ActionStep verification = beginStep(action, ActionStepType.VERIFICATION);
        emit(action, verification, AuditEventType.VERIFICATION_STARTED, AuditActorType.SYSTEM,
                Map.of("message", "Verification started"));
        ActionVerifier.VerificationResult verificationResult = resultVerifier.verify(action, primary);
        action.setVerificationPassed(verificationResult.passed());
        if (!verificationResult.passed() || !agentResult.success() || toolFailed) {
            String error = firstNonBlank(verificationResult.reason(), agentResult.responseText(),
                    action.getErrorMessage(), "Verification failed");
            failStep(verification, "VERIFICATION_FAILED", error);
            emit(action, verification, AuditEventType.VERIFICATION_FAILED, AuditActorType.SYSTEM,
                    Map.of("message", error));
            return finish(action, ActionStatus.FAILED, error, startMs, tools, AuditEventType.ACTION_FAILED);
        }
        succeedStep(verification, Map.of("passed", true));
        emit(action, verification, AuditEventType.VERIFICATION_PASSED, AuditActorType.SYSTEM,
                Map.of("message", "Verification passed"));

        ActionStep completion = beginStep(action, ActionStepType.COMPLETION);
        action.setStatus(ActionStatus.COMPLETED);
        action.setCompletedAt(Instant.now());
        action.setDurationMs(duration(startMs));
        persist(action);
        succeedStep(completion, Map.of("result", action.getResult() != null ? action.getResult() : ""));
        emit(action, completion, AuditEventType.ACTION_COMPLETED, AuditActorType.SYSTEM,
                Map.of("message", "Action completed"));
        logPipeline(action, startMs);
        return ActionExecutionResult.from(action, tools, duration(startMs));
    }

    public Action initiateAction(UUID userId, String intent, Map<String, Object> initialData) {
        ActionExecutionResult result = execute(new ActionCommand(
                userId, null, null, VoiceOsRequestContext.currentRequestId(), null, null,
                "HTTP", intent, null, initialData
        ));
        return actionStore.findById(result.actionId()).orElseGet(() -> {
            Action action = new Action(userId, intent);
            action.setStatus(ActionStatus.valueOf(result.status()));
            action.setResult(result.result());
            action.setErrorMessage(result.error());
            return action;
        });
    }

    public void processAction(Action action) {
        switch (action.getStatus()) {
            case REQUESTED, RECEIVED, UNDERSTANDING -> {
                if (actionPlanner != null) {
                    actionPlanner.plan(action);
                }
                persist(action);
                processAction(action);
            }
            case PLANNED, READY -> {
                action.setStatus(ActionStatus.EXECUTING);
                persist(action);
                processAction(action);
            }
            case REQUIRES_APPROVAL, REQUIRES_AUTHENTICATION, REQUIRES_PAYMENT, WAITING_FOR_USER ->
                    log.info("Action {} is pausing in state {}", action.getId(), action.getStatus());
            case AUTHENTICATING, EXECUTING -> {
                if (actionExecutor != null) {
                    actionExecutor.execute(action);
                }
                persist(action);
                processAction(action);
            }
            case VERIFYING, COMPLETED, FAILED, CANCELLED, REJECTED, EXPIRED ->
                    log.info("Action {} finished with state {}", action.getId(), action.getStatus());
            default -> {
            }
        }
    }

    public Action getAction(UUID actionId) {
        return actionStore.findById(actionId).orElse(null);
    }

    public Optional<Action> getActionForUser(UUID actionId, UUID userId) {
        return actionStore.findByIdAndUserId(actionId, userId);
    }

    public List<Action> listActionsForUser(UUID userId) {
        return actionStore.findByUserId(userId);
    }

    public Optional<ActionTimelineDto> timelineForUser(UUID actionId, UUID userId) {
        return getActionForUser(actionId, userId).map(action -> {
            if (actionTraceService == null) {
                return ActionTimelineDto.from(action, List.of(), List.of());
            }
            return actionTraceService.timeline(action);
        });
    }

    public Action cancel(UUID actionId, UUID userId) {
        Action action = actionStore.findByIdAndUserId(actionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Action not found"));
        if (action.getStatus() == ActionStatus.COMPLETED
                || action.getStatus() == ActionStatus.FAILED
                || action.getStatus() == ActionStatus.REJECTED) {
            return action;
        }
        action.setStatus(ActionStatus.CANCELLED);
        action.setCompletedAt(Instant.now());
        action.setErrorMessage("Cancelled");
        persist(action);
        emit(action, null, AuditEventType.ACTION_CANCELLED, AuditActorType.USER,
                Map.of("message", "Action cancelled"));
        return action;
    }

    public void resumeAction(UUID actionId, ActionStatus newStatus, Map<String, Object> newPayload) {
        Action action = getAction(actionId);
        if (action != null) {
            action.setStatus(newStatus);
            if (newPayload != null) {
                action.getPayload().putAll(newPayload);
            }
            persist(action);
            processAction(action);
        }
    }

    private ActionExecutionResult finish(Action action, ActionStatus status, String error, long startMs,
                                         List<ToolResult> tools, AuditEventType terminalEvent) {
        action.setStatus(status);
        action.setErrorMessage(error);
        if (status == ActionStatus.FAILED || status == ActionStatus.REJECTED || status == ActionStatus.CANCELLED) {
            action.setVerificationPassed(false);
        }
        action.setCompletedAt(Instant.now());
        action.setDurationMs(duration(startMs));
        persist(action);
        emit(action, null, terminalEvent, AuditActorType.SYSTEM,
                Map.of("message", error != null ? error : terminalEvent.name()));
        logPipeline(action, startMs);
        return ActionExecutionResult.from(action, tools, duration(startMs));
    }

    private void persist(Action action) {
        actionStore.save(action);
    }

    private ActionStep beginStep(Action action, ActionStepType type) {
        if (actionTraceService == null) {
            return null;
        }
        try {
            return actionTraceService.startStep(action, type);
        } catch (RuntimeException e) {
            log.error("Observability persistence failed starting step {} actionId={} ({})",
                    type, action.getId(), e.getClass().getSimpleName());
            return null;
        }
    }

    private void succeedStep(ActionStep step, Map<String, Object> result) {
        if (actionTraceService == null || step == null) {
            return;
        }
        try {
            actionTraceService.succeed(step, result);
        } catch (RuntimeException e) {
            log.error("Observability persistence failed succeeding step {} ({})",
                    step.getId(), e.getClass().getSimpleName());
        }
    }

    private void failStep(ActionStep step, String errorCode, String errorMessage) {
        if (actionTraceService == null || step == null) {
            return;
        }
        try {
            actionTraceService.fail(step, errorCode, errorMessage);
        } catch (RuntimeException e) {
            log.error("Observability persistence failed failing step {} ({})",
                    step.getId(), e.getClass().getSimpleName());
        }
    }

    private void waitUserStep(ActionStep step, String reason) {
        if (actionTraceService == null || step == null) {
            return;
        }
        try {
            actionTraceService.waitUser(step, reason);
        } catch (RuntimeException e) {
            log.error("Observability persistence failed waiting step {} ({})",
                    step.getId(), e.getClass().getSimpleName());
        }
    }

    private void emit(Action action, ActionStep step, AuditEventType type, AuditActorType actor, Map<String, Object> metadata) {
        if (actionTraceService == null) {
            return;
        }
        try {
            actionTraceService.emit(action, step, type, actor, metadata);
        } catch (RuntimeException e) {
            log.error("Observability persistence failed emitting {} actionId={} ({})",
                    type, action.getId(), e.getClass().getSimpleName());
        }
    }

    private static AuditActorType actorFor(ActionCommand command) {
        if (command.source() != null && command.source().equalsIgnoreCase("VAPI")) {
            return AuditActorType.VAPI;
        }
        return AuditActorType.USER;
    }

    private static void applyLlmMetadata(Action action, AgentResult agentResult) {
        if (agentResult == null || agentResult.stateUpdates() == null || agentResult.stateUpdates().isEmpty()) {
            return;
        }
        Map<String, Object> state = agentResult.stateUpdates();
        if (action.getTargetProvider() == null && state.get("llmProvider") != null) {
            action.setTargetProvider(String.valueOf(state.get("llmProvider")));
        }
        if (action.getProviderMode() == null && state.get("providerMode") != null) {
            action.setProviderMode(String.valueOf(state.get("providerMode")));
        }
        if (action.getPayload() == null) {
            action.setPayload(new HashMap<>());
        }
        if (state.get("intent") != null) {
            action.getPayload().put("intent", state.get("intent"));
        }
        if (state.get("model") != null) {
            action.getPayload().put("model", state.get("model"));
        }
        if (state.get("rawUserInput") != null) {
            action.getPayload().put("rawUserInput", state.get("rawUserInput"));
        }
    }

    private static void applyProviderMetadata(Action action, ToolResult primary) {
        if (primary == null || primary.data() == null) {
            return;
        }
        Object provider = primary.data().get("provider");
        if (provider != null) {
            action.setTargetProvider(String.valueOf(provider));
        }
        Object mode = primary.data().get("providerMode");
        if (mode != null) {
            action.setProviderMode(String.valueOf(mode));
        }
    }

    private UUID resolveConversation(ActionCommand command) {
        if (command.conversationId() != null) {
            return command.conversationId();
        }
        if (userRepository == null || conversationRepository == null || command.userId() == null) {
            return null;
        }
        return userRepository.findById(command.userId()).map(user -> {
            Conversation conversation = new Conversation(user, firstNonBlank(command.requestedOperation(), "Voice action"));
            if (command.callId() != null) {
                conversation.setVoiceSessionId(command.callId());
            }
            return conversationRepository.save(conversation).getId();
        }).orElse(null);
    }

    private ActionExecutionResult executeApprovedModelTools(
            Action action,
            ActionCommand command,
            Agent agent,
            AgentRequest agentRequest,
            AgentResult[] current,
            ActionStep planning,
            long startMs,
            List<ToolResult> executed
    ) {
        int max = 2;
        if (properties != null && properties.agents() != null && properties.agents().maxToolCallsPerAction() > 0) {
            max = properties.agents().maxToolCallsPerAction();
        }
        int calls = 0;
        while (pendingModelToolCall(current[0]) && calls < max) {
            AgentResult pending = current[0];
            String proposed = ToolArgumentSanitizer.sanitizeToolName(pending.selectedTool());
            Map<String, Object> args = Map.of();
            Object rawArgs = pending.stateUpdates() != null ? pending.stateUpdates().get("toolArguments") : null;
            if (rawArgs instanceof Map<?, ?> map) {
                Map<String, Object> copied = new HashMap<>();
                map.forEach((k, v) -> copied.put(String.valueOf(k), v));
                args = ToolArgumentSanitizer.sanitize(copied);
            }
            calls++;
            if (proposed == null) {
                return finish(action, ActionStatus.FAILED, "Model requested an invalid tool name.",
                        startMs, executed, AuditEventType.ACTION_FAILED);
            }

            Optional<Tool> catalog = toolRegistry.getTool(proposed);
            if (catalog.isEmpty()) {
                emit(action, planning, AuditEventType.TOOL_FAILED, AuditActorType.TOOL,
                        Map.of("message", "Unknown tool", "tool", proposed));
                return finish(action, ActionStatus.FAILED, "Unknown tool: " + proposed,
                        startMs, executed, AuditEventType.ACTION_FAILED);
            }
            Tool tool = catalog.get();
            action.setToolName(proposed);
            action.setTargetProvider(tool.getProviderName());
            action.setProviderMode(tool.getProviderMode() != null ? tool.getProviderMode().name() : "unavailable");

            ActionStep policyStep = beginStep(action, ActionStepType.POLICY_CHECK);
            PolicyDecision decision = policyEngine.evaluate(new PolicyRequest(
                    command.userId(),
                    proposed,
                    proposed,
                    tool.getRiskLevel(),
                    tool.getProviderName(),
                    args
            ));
            emit(action, policyStep, AuditEventType.POLICY_EVALUATED, AuditActorType.SYSTEM, Map.of(
                    "message", "Policy evaluated",
                    "outcome", decision.outcome().name(),
                    "reasonCode", decision.reasonCode() != null ? decision.reasonCode() : "",
                    "tool", proposed
            ));
            if (decision.isDeny()) {
                failStep(policyStep, decision.reasonCode(), decision.reason());
                emit(action, policyStep, AuditEventType.POLICY_DENIED, AuditActorType.SYSTEM,
                        Map.of("message", decision.reason() != null ? decision.reason() : "Denied",
                                "reasonCode", decision.reasonCode() != null ? decision.reasonCode() : "DENY"));
                return finish(action, ActionStatus.REJECTED, decision.reason(),
                        startMs, executed, AuditEventType.ACTION_REJECTED);
            }
            if (decision.isRequireApproval()) {
                succeedStep(policyStep, Map.of("outcome", "REQUIRE_APPROVAL"));
                ActionStep wait = beginStep(action, ActionStepType.APPROVAL_WAIT);
                waitUserStep(wait, decision.reason());
                emit(action, wait, AuditEventType.APPROVAL_REQUIRED, AuditActorType.SYSTEM,
                        Map.of("message", decision.reason() != null ? decision.reason() : "Approval required"));
                action.setErrorMessage(decision.reason());
                action.setStatus(ActionStatus.REQUIRES_APPROVAL);
                persist(action);
                logPipeline(action, startMs);
                return ActionExecutionResult.from(action, List.copyOf(executed), duration(startMs));
            }
            succeedStep(policyStep, Map.of("outcome", "ALLOW", "tool", proposed));

            if (!agent.allowsTool(proposed)) {
                return finish(action, ActionStatus.FAILED,
                        "Agent is not allowed to request tool: " + proposed,
                        startMs, executed, AuditEventType.ACTION_FAILED);
            }

            ActionStep execution = beginStep(action, ActionStepType.TOOL_EXECUTION);
            emit(action, execution, AuditEventType.TOOL_STARTED, AuditActorType.TOOL,
                    Map.of("message", "Tool started", "tool", proposed));
            emit(action, execution, AuditEventType.PROVIDER_STARTED, AuditActorType.PROVIDER, Map.of(
                    "message", "Provider started",
                    "provider", tool.getProviderName(),
                    "providerMode", tool.getProviderMode() != null ? tool.getProviderMode().name() : "unavailable"
            ));
            ToolResult toolResult = toolRegistry.executeTool(proposed, args);
            executed.add(toolResult);
            applyProviderMetadata(action, toolResult);
            if (!toolResult.success()) {
                failStep(execution, "TOOL_FAILED", toolResult.errorMessage());
                emit(action, execution, AuditEventType.PROVIDER_FAILED, AuditActorType.PROVIDER,
                        Map.of("message", toolResult.errorMessage() != null ? toolResult.errorMessage() : "Provider failed"));
                emit(action, execution, AuditEventType.TOOL_FAILED, AuditActorType.TOOL,
                        Map.of("message", toolResult.errorMessage() != null ? toolResult.errorMessage() : "Tool failed"));
                current[0] = agent.continueWith(agentRequest, List.copyOf(executed));
                applyLlmMetadata(action, current[0]);
                action.setErrorMessage(firstNonBlank(current[0].responseText(), toolResult.errorMessage()));
                action.setResult(null);
                return finish(action, ActionStatus.FAILED,
                        firstNonBlank(current[0].responseText(), toolResult.errorMessage(), "Tool failed"),
                        startMs, executed, AuditEventType.ACTION_FAILED);
            }
            succeedStep(execution, Map.of("result", toolResult.rawOutput() != null ? toolResult.rawOutput() : ""));
            emit(action, execution, AuditEventType.PROVIDER_SUCCEEDED, AuditActorType.PROVIDER, Map.of(
                    "message", "Provider succeeded",
                    "providerMode", action.getProviderMode() != null ? action.getProviderMode() : "unavailable"
            ));
            emit(action, execution, AuditEventType.TOOL_SUCCEEDED, AuditActorType.TOOL,
                    Map.of("message", "Tool succeeded", "tool", proposed));

            ActionVerifier.VerificationResult verified = resultVerifier.verify(action, toolResult);
            if (!verified.passed()) {
                return finish(action, ActionStatus.FAILED,
                        firstNonBlank(verified.reason(), "Verification failed"),
                        startMs, executed, AuditEventType.ACTION_FAILED);
            }
            current[0] = agent.continueWith(agentRequest, List.copyOf(executed));
            recordAgentOutcome(action, current[0]);
            applyLlmMetadata(action, current[0]);
        }
        if (pendingModelToolCall(current[0])) {
            return finish(action, ActionStatus.FAILED,
                    "I reached the tool-call limit before finishing this request.",
                    startMs, executed, AuditEventType.ACTION_FAILED);
        }
        return null;
    }

    private static boolean pendingModelToolCall(AgentResult result) {
        if (result == null || result.selectedTool() == null || result.selectedTool().isBlank()) {
            return false;
        }
        boolean alreadyExecuted = result.toolExecutions() != null && !result.toolExecutions().isEmpty();
        return !alreadyExecuted && result.outcome() == AgentOutcome.EXECUTE;
    }

    private static String resolveToolName(Agent agent, ActionCommand command) {
        if (command.requestedTool() != null && !command.requestedTool().isBlank()) {
            return command.requestedTool();
        }
        if (agent.ownedTools() != null && agent.ownedTools().size() == 1) {
            return agent.ownedTools().get(0).getName();
        }
        return null;
    }

    private static boolean agentOwnsTool(Agent agent, String toolName) {
        return agent != null && agent.ownsTool(toolName);
    }

    private static void recordAgentOutcome(Action action, AgentResult agentResult) {
        if (action.getPayload() == null) {
            action.setPayload(new HashMap<>());
        }
        action.getPayload().put("agentOutcome", agentResult.outcome() != null ? agentResult.outcome().name() : "unavailable");
        if (agentResult.missingFields() != null && !agentResult.missingFields().isEmpty()) {
            action.getPayload().put("missingFields", List.copyOf(agentResult.missingFields()));
        }
        if (agentResult.errorCode() != null) {
            action.getPayload().put("errorCode", agentResult.errorCode());
        }
    }

    private void applyRequestContext(Action action) {
        VoiceOsRequestContext current = VoiceOsRequestContext.current();
        if (current != null) {
            VoiceOsRequestContext.set(current
                    .withUserId(action.getUserId())
                    .withConversation(action.getConversationId())
                    .withCall(action.getCallId(), action.getEventId()));
        }
        if (action.getUserId() != null) {
            MDC.put(RequestCorrelationFilter.MDC_USER_ID, action.getUserId().toString());
        }
        if (action.getCallId() != null) {
            MDC.put(RequestCorrelationFilter.MDC_CALL_ID, action.getCallId());
        }
    }

    private void logPipeline(Action action, long startMs) {
        log.info("action pipeline requestId={} userId={} conversationId={} callId={} actionId={} eventId={} agent={} tool={} provider={} status={} durationMs={}",
                action.getRequestId(),
                action.getUserId(),
                action.getConversationId(),
                action.getCallId(),
                action.getId(),
                action.getEventId(),
                action.getTargetAgent(),
                action.getToolName(),
                action.getTargetProvider(),
                action.getStatus(),
                duration(startMs));
    }

    private static long duration(long startMs) {
        return System.currentTimeMillis() - startMs;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
