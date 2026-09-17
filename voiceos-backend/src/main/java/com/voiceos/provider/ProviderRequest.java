package com.voiceos.provider;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provider input. Tools translate their params here. Clients cannot select
 * an internal provider implementation through this object.
 */
public record ProviderRequest(
        String operation,
        Map<String, Object> params,
        Long timeoutMs
) {
    public ProviderRequest {
        params = params != null ? new LinkedHashMap<>(params) : Map.of();
        operation = operation != null && !operation.isBlank() ? operation : "execute";
    }

    public Object param(String key) {
        return params.get(key);
    }

    public String paramAsString(String key) {
        Object value = params.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    public long timeoutOr(long fallback) {
        return timeoutMs != null && timeoutMs > 0 ? timeoutMs : fallback;
    }
}
