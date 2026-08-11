package com.voiceos.ai;

import java.util.List;
import java.util.Map;

/**
 * Structured request to the LLM provider.
 *
 * @param systemPrompt   the system-level instruction for the LLM
 * @param userMessage    the user's current message
 * @param messageHistory prior messages in the conversation (role → content pairs)
 * @param maxTokens      maximum tokens in the response (null = provider default)
 * @param temperature    creativity setting 0.0-1.0 (null = provider default)
 * @param options        provider-specific options (e.g. function definitions)
 */
public record LLMRequest(
        String systemPrompt,
        String userMessage,
        List<LLMMessage> messageHistory,
        Integer maxTokens,
        Double temperature,
        Map<String, Object> options
) {

    /** Simple factory for one-shot requests without history. */
    public static LLMRequest simple(String systemPrompt, String userMessage) {
        return new LLMRequest(systemPrompt, userMessage, List.of(), null, null, Map.of());
    }

    /** Factory for conversation requests with message history. */
    public static LLMRequest withHistory(String systemPrompt, String userMessage,
                                         List<LLMMessage> history) {
        return new LLMRequest(systemPrompt, userMessage, history, null, null, Map.of());
    }

    /** Convenience record for conversation messages. */
    public record LLMMessage(String role, String content) {}
}
