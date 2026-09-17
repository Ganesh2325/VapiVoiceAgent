package com.voiceos.action.audit;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Strips credentials from step/audit metadata. Never persist JWTs, secrets, or payment data.
 */
public final class SensitiveDataRedactor {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "passwordhash", "password_hash", "jwt", "token", "accesstoken", "access_token",
            "refreshtoken", "refresh_token", "authorization", "secret", "apikey", "api_key", "webhooksecret",
            "webhook_secret", "vapiapikey", "vapi_api_key", "cookie", "otp", "card", "cardnumber", "cvv",
            "cvc", "pan", "ssn"
    );

    private static final Pattern JWT_SHAPE = Pattern.compile("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$");

    private SensitiveDataRedactor() {}

    public static Map<String, Object> redact(Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key != null && isSensitiveKey(key)) {
                out.put(key, "[REDACTED]");
            } else if (value instanceof Map<?, ?> nested) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) nested;
                out.put(key, redact(nestedMap));
            } else if (value instanceof String text && looksLikeSecret(text)) {
                out.put(key, "[REDACTED]");
            } else {
                out.put(key, value);
            }
        }
        return out;
    }

    static boolean isSensitiveKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
        if (SENSITIVE_KEYS.contains(normalized) || SENSITIVE_KEYS.contains(key.toLowerCase(Locale.ROOT))) {
            return true;
        }
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.contains("password") || lower.contains("secret") || lower.contains("token")
                || lower.contains("authorization") || lower.endsWith("key") && lower.contains("api");
    }

    private static boolean looksLikeSecret(String value) {
        if (value == null || value.length() < 20) {
            return false;
        }
        return JWT_SHAPE.matcher(value).matches()
                || value.toLowerCase(Locale.ROOT).startsWith("bearer ");
    }
}
