package com.voiceos.agent.core;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Deterministic intent labels from English keywords. Not a semantic or multilingual router.
 */
@Component
public class KeywordIntentClassifier implements IntentClassifier {

    private static final Pattern ARITHMETIC = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*([*x×]|\\+|plus|times|multiplied|divided)\\s*(?:by\\s*)?(\\d+(?:\\.\\d+)?)",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String classify(AgentRequest request) {
        String raw = request != null ? request.rawUserInput() : null;
        if (raw == null || raw.isBlank()) {
            return "UNKNOWN";
        }
        String lower = raw.toLowerCase(Locale.ROOT);
        if (ARITHMETIC.matcher(raw).find()
                || lower.contains("calculate")
                || lower.contains("product of")
                || lower.contains("multiplied")
                || lower.contains("times ")) {
            return "CALCULATION";
        }
        if (lower.contains("flight") || lower.contains("hotel") || lower.contains("trip") || lower.contains("travel")) {
            return "TRAVEL";
        }
        if ((lower.contains("send") && lower.contains("email")) || lower.contains("email to")) {
            return "EMAIL";
        }
        if (lower.contains("write") || lower.contains("draft") || lower.contains("compose")) {
            return "WRITING";
        }
        if (lower.contains("explain") || lower.contains("difference") || lower.startsWith("why ")) {
            return "EXPLANATION";
        }
        if (lower.contains("weather") || lower.contains("stock") || lower.contains("latest news")) {
            return "REALTIME";
        }
        return "GENERAL_QUESTION";
    }
}
