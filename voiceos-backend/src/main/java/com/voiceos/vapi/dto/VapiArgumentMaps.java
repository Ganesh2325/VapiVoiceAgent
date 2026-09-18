package com.voiceos.vapi.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads Vapi tool arguments from the shapes Vapi actually sends:
 * a JSON object, or a JSON object encoded as a string.
 */
final class VapiArgumentMaps {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

    private VapiArgumentMaps() {}

    static Map<String, Object> from(JsonNode node, ObjectMapper mapper) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return Map.of();
        }
        if (node.isTextual()) {
            String raw = node.asText();
            if (raw == null || raw.isBlank()) {
                return Map.of();
            }
            try {
                Map<String, Object> parsed = mapper.readValue(raw, MAP);
                return parsed != null ? parsed : Map.of();
            } catch (JsonProcessingException e) {
                return Map.of();
            }
        }
        if (node.isObject()) {
            Map<String, Object> parsed = mapper.convertValue(node, MAP);
            return parsed != null ? parsed : Map.of();
        }
        return Map.of();
    }

    static Map<String, Object> copy(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(source);
    }
}
