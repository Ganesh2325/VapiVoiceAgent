package com.voiceos.vapi.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiFunction;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiToolCall;

import java.io.IOException;
import java.util.Map;

/**
 * Maps both Vapi tool-call encodings onto the existing {@link VapiToolCall} record:
 * nested {@code function.name}/{@code function.arguments}, and live
 * {@code toolCallList} items with top-level {@code name}/{@code arguments|parameters}.
 */
public class VapiToolCallDeserializer extends StdDeserializer<VapiToolCall> {

    public VapiToolCallDeserializer() {
        super(VapiToolCall.class);
    }

    @Override
    public VapiToolCall deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        JsonNode node = mapper.readTree(parser);
        if (node == null || node.isNull()) {
            return new VapiToolCall(null, null, null);
        }

        String id = text(node, "id");
        String type = text(node, "type");
        if (type == null || type.isBlank()) {
            type = "function";
        }

        String name = null;
        Map<String, Object> arguments = Map.of();
        JsonNode functionNode = node.get("function");
        if (functionNode != null && functionNode.isObject()) {
            name = text(functionNode, "name");
            arguments = VapiArgumentMaps.from(firstPresent(functionNode, "arguments", "parameters"), mapper);
        }
        if (name == null || name.isBlank()) {
            name = text(node, "name");
        }
        if (arguments.isEmpty()) {
            arguments = VapiArgumentMaps.from(firstPresent(node, "arguments", "parameters"), mapper);
        }

        return new VapiToolCall(id, type, new VapiFunction(name, VapiArgumentMaps.copy(arguments)));
    }

    private static JsonNode firstPresent(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull() && !value.isMissingNode()) {
                return value;
            }
        }
        return null;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.isTextual()) {
            return null;
        }
        String text = value.asText();
        return text != null && !text.isBlank() ? text : null;
    }
}
