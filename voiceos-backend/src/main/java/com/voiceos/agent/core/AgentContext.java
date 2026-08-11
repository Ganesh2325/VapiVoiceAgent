package com.voiceos.agent.core;

import com.voiceos.domain.entity.Conversation;
import com.voiceos.domain.entity.User;
import com.voiceos.domain.entity.Message;
import com.voiceos.domain.entity.Memory;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Execution context passed to agents during an execution cycle.
 * Contains user input, conversation history, user preferences, long-term memory, and session state.
 *
 * @param conversationId current conversation UUID
 * @param userId         current user UUID
 * @param userInput      the current text or transcribed voice instruction
 * @param history        recent messages in the current conversation
 * @param memories       relevant long-term / episodic memories retrieved for this user
 * @param stateVariables dynamic state map shared between agents during multi-agent handoffs
 */
public record AgentContext(
        UUID conversationId,
        UUID userId,
        String userInput,
        List<Message> history,
        List<Memory> memories,
        Map<String, Object> stateVariables
) {

    public static AgentContext of(UUID conversationId, UUID userId, String userInput) {
        return new AgentContext(conversationId, userId, userInput, List.of(), List.of(), new java.util.HashMap<>());
    }

    public static AgentContext of(UUID conversationId, UUID userId, String userInput,
                                  List<Message> history, List<Memory> memories) {
        return new AgentContext(conversationId, userId, userInput, history, memories, new java.util.HashMap<>());
    }
}
