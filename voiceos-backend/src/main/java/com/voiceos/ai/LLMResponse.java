package com.voiceos.ai;

/**
 * Structured response from the LLM provider.
 *
 * @param content      the generated text content
 * @param providerName which provider produced this response
 * @param model        the specific model used (e.g. "gemini-2.0-flash")
 * @param inputTokens  number of input tokens consumed (null if unavailable)
 * @param outputTokens number of output tokens generated (null if unavailable)
 * @param finishReason how the generation was completed (e.g. "stop", "length")
 * @param latencyMs    time taken for the LLM call in milliseconds
 */
public record LLMResponse(
        String content,
        String providerName,
        String model,
        Integer inputTokens,
        Integer outputTokens,
        String finishReason,
        long latencyMs
) {

    /** Total tokens used (input + output). Returns -1 if token counts unavailable. */
    public int totalTokens() {
        if (inputTokens == null || outputTokens == null) return -1;
        return inputTokens + outputTokens;
    }

    /** Whether the response was truncated. */
    public boolean wasTruncated() {
        return "length".equals(finishReason);
    }
}
