package com.voiceos.agent.decision;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Server-side sanitizer for untrusted model tool arguments.
 * Strips injection fields and enforces size limits. Does not execute anything.
 */
public final class ToolArgumentSanitizer {

    public static final int MAX_STRING_LENGTH = 500;
    public static final int MAX_KEYS = 16;

    private static final Set<String> PROHIBITED = Set.of(
            "provider", "providername", "providerclass", "url", "endpoint", "uri",
            "authorization", "jwt", "password", "otp", "cardnumber", "token",
            "agent", "agentname", "sql", "command", "classname", "httpmethod",
            "http", "shell", "filesystem", "filepath", "credential", "secret",
            "apikey", "api_key"
    );

    private ToolArgumentSanitizer() {}

    public static Map<String, Object> sanitize(Map<String, Object> raw) {
        Map<String, Object> clean = new LinkedHashMap<>();
        if (raw == null) {
            return clean;
        }
        int count = 0;
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            String key = entry.getKey().trim();
            if (key.isBlank() || PROHIBITED.contains(key.toLowerCase(Locale.ROOT))) {
                continue;
            }
            if (count >= MAX_KEYS) {
                break;
            }
            Object value = entry.getValue();
            if (value instanceof String str) {
                if (str.length() > MAX_STRING_LENGTH) {
                    value = str.substring(0, MAX_STRING_LENGTH);
                }
            } else if (value instanceof Map<?, ?>) {
                continue;
            } else if (value instanceof Iterable<?>) {
                continue;
            }
            clean.put(key, value);
            count++;
        }
        return clean;
    }

    public static String sanitizeToolName(String toolName) {
        if (toolName == null) {
            return null;
        }
        String trimmed = toolName.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isBlank() || trimmed.length() > 64) {
            return null;
        }
        if (!trimmed.matches("[a-z][a-z0-9_]*")) {
            return null;
        }
        return trimmed;
    }
}
