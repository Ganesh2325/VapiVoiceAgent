package com.voiceos.vapi.service;

import com.voiceos.action.engine.ActionEngine;
import com.voiceos.action.model.ActionCommand;
import com.voiceos.action.model.ActionExecutionResult;
import com.voiceos.security.VoiceOsRequestContext;
import com.voiceos.service.VoiceSessionService;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiCall;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiFunction;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiMessage;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiToolCall;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiToolCallResponse;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Maps Vapi tool-calls onto the canonical ActionEngine pipeline.
 * Does not execute tools directly.
 */
@Service
public class VapiToolService {

    private static final Logger log = LoggerFactory.getLogger(VapiToolService.class);

    private final ActionEngine actionEngine;
    private final VoiceSessionService voiceSessionService;

    public VapiToolService(ActionEngine actionEngine, VoiceSessionService voiceSessionService) {
        this.actionEngine = actionEngine;
        this.voiceSessionService = voiceSessionService;
    }

    public VapiToolCallResponse handleToolCalls(VapiMessage message, VapiCall call) {
        List<VapiToolCall> calls = resolveToolCalls(message);
        if (calls.isEmpty()) {
            return VapiToolCallResponse.of(List.of());
        }

        List<VapiToolResult> results = new ArrayList<>();
        for (VapiToolCall toolCall : calls) {
            results.add(executeSingleToolCall(toolCall, call));
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
        Map<String, Object> args = func.arguments() != null ? new HashMap<>(func.arguments()) : new HashMap<>();
        String vapiCallId = call != null ? call.id() : null;

        UUID userId = voiceSessionService.resolveBoundUserId(vapiCallId).orElse(null);
        UUID conversationId = voiceSessionService.resolveConversationId(vapiCallId).orElse(null);

        String requestedTool = requestedToolName(functionName);
        String operation = operationText(functionName, args);

        ActionCommand command = new ActionCommand(
                userId,
                conversationId,
                vapiCallId,
                VoiceOsRequestContext.currentRequestId(),
                VoiceOsRequestContext.current() != null ? VoiceOsRequestContext.current().eventId() : null,
                actionIdempotencyKey(vapiCallId, callId),
                "VAPI",
                operation,
                requestedTool,
                args
        );

        log.info("Vapi tool-call routed to ActionEngine function={} callId={} toolCallId={} userBound={}",
                functionName, vapiCallId, callId, userId != null);

        ActionExecutionResult result = actionEngine.execute(command);
        return toVapiResult(callId, functionName, result);
    }

    private static VapiToolResult toVapiResult(String toolCallId, String functionName, ActionExecutionResult result) {
        if (result.awaitingApproval()) {
            return VapiToolResult.success(
                    toolCallId,
                    functionName,
                    "This action requires approval before it can run. Status: REQUIRES_APPROVAL."
            );
        }
        if (!result.success()) {
            String error = result.error() != null ? result.error() : "Action failed with status " + result.status();
            return VapiToolResult.error(toolCallId, functionName, error);
        }
        String voice = result.responseText() != null ? result.responseText() : result.result();
        return VapiToolResult.success(toolCallId, functionName, voice);
    }

    private static List<VapiToolCall> resolveToolCalls(VapiMessage message) {
        if (hasToolCalls(message.toolCalls())) {
            return message.toolCalls();
        }
        if (hasToolCalls(message.toolCallList())) {
            return message.toolCallList();
        }
        if (message.functionCall() != null) {
            return List.of(new VapiToolCall("single-call", "function", message.functionCall()));
        }
        return List.of();
    }

    private static boolean hasToolCalls(List<VapiToolCall> calls) {
        return calls != null && !calls.isEmpty();
    }

    private static String requestedToolName(String functionName) {
        if (functionName.equals("execute_agent_action")
                || functionName.equals("voiceos_orchestrator")
                || functionName.equals("voiceos_request")
                || functionName.equals("general_query")
                || functionName.equals("answer_question")) {
            return null;
        }
        if (functionName.startsWith("agent_") || functionName.endsWith("_agent")) {
            return null;
        }
        return functionName;
    }

    private static String operationText(String functionName, Map<String, Object> args) {
        Object input = args.get("input");
        if (input == null) {
            input = args.get("query");
        }
        if (input == null) {
            input = args.get("utterance");
        }
        if (input == null) {
            input = args.get("expression");
        }
        if (input != null) {
            return String.valueOf(input);
        }
        return functionName;
    }

    private static String actionIdempotencyKey(String vapiCallId, String toolCallId) {
        return "action:vapi:" + (vapiCallId != null ? vapiCallId : "none") + ":" + toolCallId;
    }
}
