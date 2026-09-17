package com.voiceos.provider;

import com.voiceos.action.audit.SensitiveDataRedactor;
import com.voiceos.tool.core.ToolResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps {@link ProviderResult} to {@link ToolResult} without converting failure into success.
 */
public final class ProviderResults {

    private ProviderResults() {}

    public static ToolResult toToolResult(ProviderResult result) {
        if (result == null) {
            return ToolResult.failure("Provider returned no result", 0);
        }
        Map<String, Object> data = SensitiveDataRedactor.redact(result.toSafeMap());
        if (result.success()) {
            return ToolResult.success(data, result.message(), result.durationMs());
        }
        String code = result.errorCode() != null ? result.errorCode().name() : result.outcome().name();
        String message = result.message() != null ? result.message() : code;
        return ToolResult.failure(data, code + ": " + message, result.durationMs());
    }

    public static Map<String, Object> simulatedData(Map<String, Object> extra) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "SIMULATED");
        if (extra != null) {
            data.putAll(extra);
        }
        return data;
    }
}
