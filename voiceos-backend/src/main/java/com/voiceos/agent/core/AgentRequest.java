package com.voiceos.agent.core;

import com.voiceos.action.model.ActionCommand;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Structured agent input. Does not carry JWTs, secrets, OTP, cards, or credentials.
 */
public record AgentRequest(
        UUID userId,
        UUID actionId,
        String requestId,
        String callId,
        UUID conversationId,
        String rawUserInput,
        String requestedTool,
        Map<String, Object> parameters,
        String locale
) {
    public AgentRequest {
        parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
        parameters.remove("provider");
        parameters.remove("providerName");
        parameters.remove("providerClass");
        parameters.remove("agent");
        parameters.remove("agentName");
        parameters.remove("jwt");
        parameters.remove("authorization");
        parameters.remove("password");
        parameters.remove("otp");
        parameters.remove("cardNumber");
        parameters.remove("token");
    }

    public AgentContext toContext() {
        Map<String, Object> state = new HashMap<>();
        if (requestedTool != null && !requestedTool.isBlank()) {
            state.put("requestedTool", requestedTool);
        }
        state.put("toolParams", parameters);
        if (actionId != null) {
            state.put("actionId", actionId.toString());
        }
        if (requestId != null) {
            state.put("requestId", requestId);
        }
        if (callId != null) {
            state.put("callId", callId);
        }
        return new AgentContext(conversationId, userId, rawUserInput, List.of(), List.of(), state);
    }

    public static AgentRequest from(AgentContext context, String requestedTool) {
        if (context == null) {
            return new AgentRequest(null, null, null, null, null, null, requestedTool, Map.of(), null);
        }
        Map<String, Object> params = Map.of();
        if (context.stateVariables() != null && context.stateVariables().get("toolParams") instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cast = (Map<String, Object>) map;
            params = cast;
        }
        String tool = requestedTool;
        if (tool == null && context.stateVariables() != null && context.stateVariables().get("requestedTool") != null) {
            tool = String.valueOf(context.stateVariables().get("requestedTool"));
        }
        return new AgentRequest(
                context.userId(),
                null,
                null,
                null,
                context.conversationId(),
                context.userInput(),
                tool,
                params,
                null
        );
    }

    public static AgentRequest from(ActionCommand command, UUID actionId) {
        if (command == null) {
            return new AgentRequest(null, actionId, null, null, null, null, null, Map.of(), null);
        }
        String input = command.requestedOperation() != null ? command.requestedOperation() : command.requestedTool();
        return new AgentRequest(
                command.userId(),
                actionId,
                command.requestId(),
                command.callId(),
                command.conversationId(),
                input,
                command.requestedTool(),
                command.params(),
                null
        );
    }

    public String parameterAsString(String key) {
        Object value = parameters.get(key);
        return value != null ? String.valueOf(value) : null;
    }
}
