package com.voiceos.ai;

import java.util.List;
import java.util.Map;

/**
 * LLM Provider abstraction.
 *
 * <p>All LLM interactions go through this interface.
 * This decouples the application from any specific AI vendor.
 * Implementations: {@link com.voiceos.ai.gemini.GeminiLLMProvider},
 *                  {@link com.voiceos.ai.groq.GroqLLMProvider}.
 *
 * <p>Provider selection is controlled by the {@code voiceos.ai.provider} property.
 */
public interface LLMProvider {

    /**
     * Sends a chat request to the LLM and returns the response.
     *
     * @param request the structured chat request
     * @return the LLM response
     */
    LLMResponse chat(LLMRequest request);

    /**
     * Generates an embedding vector for the given text.
     * Used for RAG and semantic memory.
     *
     * @param text the input text to embed
     * @return list of float values representing the embedding
     */
    List<Float> embed(String text);

    /**
     * @return the provider name (e.g. "gemini", "groq")
     */
    String getProviderName();

    /**
     * @return whether this provider is currently reachable and operational
     */
    boolean isAvailable();
}
