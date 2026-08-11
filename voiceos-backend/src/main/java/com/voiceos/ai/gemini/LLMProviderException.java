package com.voiceos.ai.gemini;

/**
 * Exception thrown when an LLM provider call fails.
 * Wraps provider-specific exceptions into a unified exception type.
 */
public class LLMProviderException extends RuntimeException {

    public LLMProviderException(String message) {
        super(message);
    }

    public LLMProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
