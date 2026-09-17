package com.voiceos.provider;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Honest provider outcome. {@code success} is true only for {@link ProviderOutcome#SUCCESS}.
 */
public record ProviderResult(
        ProviderOutcome outcome,
        ProviderMode mode,
        String providerName,
        String operation,
        Map<String, Object> data,
        String message,
        ProviderErrorCode errorCode,
        long durationMs
) {
    public ProviderResult {
        data = data != null ? Map.copyOf(data) : Map.of();
        outcome = outcome != null ? outcome : ProviderOutcome.FAILURE;
        mode = mode != null ? mode : ProviderMode.MOCK;
    }

    public boolean success() {
        return outcome == ProviderOutcome.SUCCESS;
    }

    public static ProviderResult success(String providerName, ProviderMode mode, String operation,
                                         Map<String, Object> data, String message, long durationMs) {
        return new ProviderResult(ProviderOutcome.SUCCESS, mode, providerName, operation, data, message, null, durationMs);
    }

    public static ProviderResult failure(String providerName, ProviderMode mode, String operation,
                                         ProviderErrorCode errorCode, String message, long durationMs) {
        return new ProviderResult(ProviderOutcome.FAILURE, mode, providerName, operation, Map.of(), message, errorCode, durationMs);
    }

    public static ProviderResult failure(String providerName, ProviderMode mode, String operation,
                                         Map<String, Object> data, ProviderErrorCode errorCode,
                                         String message, long durationMs) {
        return new ProviderResult(ProviderOutcome.FAILURE, mode, providerName, operation, data, message, errorCode, durationMs);
    }

    public static ProviderResult unavailable(String providerName, ProviderMode mode, String operation,
                                             ProviderErrorCode errorCode, String message, long durationMs) {
        return new ProviderResult(ProviderOutcome.UNAVAILABLE, mode, providerName, operation, Map.of(), message, errorCode, durationMs);
    }

    public static ProviderResult timeout(String providerName, ProviderMode mode, String operation,
                                         String message, long durationMs) {
        return new ProviderResult(ProviderOutcome.TIMEOUT, mode, providerName, operation, Map.of(),
                message != null ? message : "Provider timed out", ProviderErrorCode.TIMEOUT, durationMs);
    }

    public Map<String, Object> toSafeMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("provider", providerName);
        map.put("providerMode", mode != null ? mode.name() : null);
        map.put("outcome", outcome != null ? outcome.name() : null);
        map.put("operation", operation);
        if (errorCode != null) {
            map.put("errorCode", errorCode.name());
        }
        map.putAll(data);
        return map;
    }
}
