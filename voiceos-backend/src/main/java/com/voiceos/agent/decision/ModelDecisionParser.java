package com.voiceos.agent.decision;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses model text into {@link ModelDecision}. Arbitrary prose is not treated as a tool call.
 */
public final class ModelDecisionParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ModelDecisionParser() {}

    public static ModelDecision parse(String content) {
        if (content == null || content.isBlank()) {
            return new ModelDecision("FAILED", null, "UNKNOWN", null, Map.of(), List.of());
        }
        String json = extractJsonObject(content);
        if (json == null) {
            return new ModelDecision("ANSWER", content.trim(), "GENERAL_QUESTION", null, Map.of(), List.of());
        }
        try {
            JsonNode root = MAPPER.readTree(json);
            if (root == null || !root.isObject()) {
                return new ModelDecision("ANSWER", content.trim(), "GENERAL_QUESTION", null, Map.of(), List.of());
            }
            String outcome = text(root, "outcome");
            String answer = text(root, "answer");
            String intent = text(root, "intent");
            JsonNode toolCall = root.get("toolCall");
            String toolName = null;
            Map<String, Object> arguments = new LinkedHashMap<>();
            if (toolCall != null && toolCall.isObject()) {
                toolName = ToolArgumentSanitizer.sanitizeToolName(text(toolCall, "toolName"));
                arguments = ToolArgumentSanitizer.sanitize(toMap(toolCall.get("arguments")));
            }
            if (toolName == null) {
                toolName = ToolArgumentSanitizer.sanitizeToolName(text(root, "toolName"));
            }
            if (arguments.isEmpty()) {
                arguments = ToolArgumentSanitizer.sanitize(toMap(root.get("arguments")));
            }
            List<String> missing = toStringList(root.get("missingFields"));
            if (outcome == null || outcome.isBlank()) {
                outcome = toolName != null ? "TOOL" : (answer != null ? "ANSWER" : "FAILED");
            }
            return new ModelDecision(outcome.trim().toUpperCase(), answer, intent, toolName, arguments, missing);
        } catch (Exception e) {
            return new ModelDecision("ANSWER", content.trim(), "GENERAL_QUESTION", null, Map.of(), List.of());
        }
    }

    static String extractJsonObject(String content) {
        String stripped = content.trim();
        if (stripped.startsWith("```")) {
            int firstNl = stripped.indexOf('\n');
            int fence = stripped.lastIndexOf("```");
            if (firstNl > 0 && fence > firstNl) {
                stripped = stripped.substring(firstNl + 1, fence).trim();
            }
        }
        int start = stripped.indexOf('{');
        int end = stripped.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return stripped.substring(start, end + 1);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.isValueNode()) {
            return null;
        }
        String text = value.asText();
        return text != null && !text.isBlank() ? text : null;
    }

    private static Map<String, Object> toMap(JsonNode node) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (node == null || !node.isObject()) {
            return map;
        }
        node.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            if (value == null || value.isNull() || value.isObject() || value.isArray()) {
                return;
            }
            if (value.isNumber()) {
                map.put(entry.getKey(), value.numberValue());
            } else if (value.isBoolean()) {
                map.put(entry.getKey(), value.booleanValue());
            } else {
                map.put(entry.getKey(), value.asText());
            }
        });
        return map;
    }

    private static List<String> toStringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return values;
        }
        for (JsonNode item : node) {
            if (item != null && item.isValueNode() && !item.asText().isBlank()) {
                values.add(item.asText());
            }
        }
        return values;
    }
}
