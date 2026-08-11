package com.voiceos.vapi.service;

import com.voiceos.agent.core.AgentContext;
import com.voiceos.agent.core.AgentRegistry;
import com.voiceos.agent.core.AgentResult;
import com.voiceos.domain.entity.Approval;
import com.voiceos.domain.repository.ApprovalRepository;
import com.voiceos.orchestrator.OrchestratorService;
import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.ToolRegistry;
import com.voiceos.tool.core.ToolResult;
import com.voiceos.vapi.dto.VapiWebhookDTOs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service responsible for executing tool calls requested by the Vapi Voice Agent.
 * Connects Vapi directly to Spring Boot's multi-agent orchestrator, domain agents,
 * and deterministic tools.
 */
@Service
public class VapiToolService {

    private static final Logger log = LoggerFactory.getLogger(VapiToolService.class);

    private final ToolRegistry toolRegistry;
    private final AgentRegistry agentRegistry;
    private final OrchestratorService orchestratorService;
    private final ApprovalRepository approvalRepository;

    public VapiToolService(
            ToolRegistry toolRegistry,
            AgentRegistry agentRegistry,
            OrchestratorService orchestratorService,
            ApprovalRepository approvalRepository
    ) {
        this.toolRegistry = toolRegistry;
        this.agentRegistry = agentRegistry;
        this.orchestratorService = orchestratorService;
        this.approvalRepository = approvalRepository;
    }

    /**
     * Executes all tool calls in the Vapi payload and returns formatted results for voice synthesis.
     */
    public VapiToolCallResponse handleToolCalls(VapiMessage message, VapiCall call) {
        List<VapiToolCall> calls = message.toolCalls() != null ? message.toolCalls() : message.toolCallList();
        if (calls == null || calls.isEmpty()) {
            if (message.functionCall() != null) {
                // Fallback for single function call
                calls = List.of(new VapiToolCall("single-call", "function", message.functionCall()));
            } else {
                return VapiToolCallResponse.of(List.of());
            }
        }

        List<VapiToolResult> results = new ArrayList<>();
        for (VapiToolCall toolCall : calls) {
            VapiToolResult result = executeSingleToolCall(toolCall, call);
            results.add(result);
        }

        return VapiToolCallResponse.of(results);
    }

    private VapiToolResult executeSingleToolCall(VapiToolCall toolCall, VapiCall call) {
        String callId = toolCall.id() != null ? toolCall.id() : UUID.randomUUID().toString();
        VapiFunction func = toolCall.function();
        if (func == null || func.name() == null) {
            return VapiToolResult.error(callId, "unknown", "Function name is missing.");
        }

        String functionName = func.name().toLowerCase().trim();
        Map<String, Object> args = func.arguments() != null ? func.arguments() : Map.of();
        log.info("Executing Vapi Tool Call: '{}' with arguments: {}", functionName, args);

        try {
            // Case 1: Universal Orchestrator Execution Tool
            if (functionName.equals("execute_agent_action") || functionName.equals("voiceos_orchestrator")) {
                String input = (String) args.getOrDefault("input", (String) args.get("query"));
                if (input == null || input.isBlank()) {
                    return VapiToolResult.error(callId, functionName, "Please provide an instruction or input for the agent.");
                }

                UUID convId = parseConversationId(call);
                var orchestratorResult = orchestratorService.execute(convId, input);

                String voiceResponse = orchestratorResult.responseText();
                if (orchestratorResult.awaitingApproval()) {
                    voiceResponse = "I have prepared this action. Because it is a high-risk operation, please confirm if you would like me to proceed.";
                }
                return VapiToolResult.success(callId, functionName, voiceResponse);
            }

            // Case 2: Specialized Agent Routing by Name
            if (functionName.startsWith("agent_") || functionName.endsWith("_agent")) {
                String normalizedAgentName = functionName.replace("agent_", "").replace("_agent", "");
                String input = (String) args.getOrDefault("input", (String) args.get("query"));

                UUID convId = parseConversationId(call);
                AgentContext ctx = AgentContext.of(convId, null, input != null ? input : "Execute standard agent task");

                for (var agent : agentRegistry.getAllAgents()) {
                    if (agent.getName().toLowerCase().contains(normalizedAgentName)) {
                        AgentResult agentResult = agent.execute(ctx);
                        return VapiToolResult.success(callId, functionName, agentResult.responseText());
                    }
                }
            }

            // Case 3: Direct Deterministic Tool in ToolRegistry
            Optional<Tool> optionalTool = toolRegistry.getTool(functionName);
            if (optionalTool.isPresent()) {
                Tool tool = optionalTool.get();

                // Check Human-in-the-Loop Risk Filter
                if (tool.getRiskLevel().requiresHumanApproval()) {
                    // Check if confirmation was explicitly passed
                    boolean confirmed = Boolean.parseBoolean(String.valueOf(args.getOrDefault("confirmed", "false")));
                    if (!confirmed) {
                        // Register approval request in database
                        Approval approval = new Approval();
                        approval.setActionType(functionName.toUpperCase());
                        approval.setActionDescription("Vapi Voice Agent requested: " + functionName + " with params: " + args);
                        approval.setPayload(args);
                        approval.setRiskLevel(com.voiceos.domain.entity.ToolExecution.RiskLevel.HIGH);
                        approval.setStatus(Approval.ApprovalStatus.PENDING);
                        approvalRepository.save(approval);

                        return VapiToolResult.success(
                                callId,
                                functionName,
                                "I have prepared this action, but it involves sensitive operations. Would you like me to approve and proceed?"
                        );
                    }
                }

                // Execute deterministic tool
                ToolResult result = tool.execute(args);
                if (result.success()) {
                    String voiceOutput = formatToolOutputForVoice(functionName, result);
                    return VapiToolResult.success(callId, functionName, voiceOutput);
                } else {
                    return VapiToolResult.error(callId, functionName, "Tool execution failed: " + result.errorMessage());
                }
            }

            // Unknown tool fallback
            log.warn("Unknown tool name requested by Vapi: {}", functionName);
            return VapiToolResult.error(callId, functionName, "Tool '" + functionName + "' is not registered in VoiceOS.");

        } catch (Exception e) {
            log.error("Exception executing Vapi tool call '{}': {}", functionName, e.getMessage(), e);
            return VapiToolResult.error(callId, functionName, "Error executing tool: " + e.getMessage());
        }
    }

    private String formatToolOutputForVoice(String toolName, ToolResult result) {
        if (result.rawOutput() != null && !result.rawOutput().isBlank()) {
            return result.rawOutput();
        }
        if (result.data() != null && !result.data().isEmpty()) {
            return "Operation completed successfully: " + result.data().toString();
        }
        return "The requested action was completed successfully.";
    }

    private UUID parseConversationId(VapiCall call) {
        if (call != null && call.id() != null) {
            try {
                return UUID.nameUUIDFromBytes(call.id().getBytes());
            } catch (Exception ignored) {}
        }
        return UUID.randomUUID();
    }
}
