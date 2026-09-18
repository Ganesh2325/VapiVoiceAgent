package com.voiceos.ai;

import com.voiceos.ai.gemini.LLMProviderException;

import java.util.List;

/**
 * Honest stand-in when a real LLM is requested but not configured.
 * Never silently answers and never pretends to be MOCK or REAL.
 */
public class UnavailableLLMProvider implements LLMProvider {

    private final String requestedProvider;

    public UnavailableLLMProvider(String requestedProvider) {
        this.requestedProvider = requestedProvider != null && !requestedProvider.isBlank()
                ? requestedProvider
                : "unconfigured";
    }

    @Override
    public LLMResponse chat(LLMRequest request) {
        throw new LLMProviderException("LLM provider '" + requestedProvider + "' is not configured");
    }

    @Override
    public List<Float> embed(String text) {
        throw new LLMProviderException("LLM provider '" + requestedProvider + "' is not configured");
    }

    @Override
    public String getProviderName() {
        return "unavailable";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
