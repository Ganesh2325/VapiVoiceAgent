package com.voiceos.vapi.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiFunction;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiToolCall;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiWebhookPayload;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class VapiLiveContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void liveToolCallListUsesTopLevelNameAndArguments() throws Exception {
        String json = """
                {
                  "message": {
                    "type": "tool-calls",
                    "toolCallList": [
                      {
                        "id": "toolu_01DTPAzUm5Gk3zxrpJ969oMF",
                        "name": "calculator",
                        "arguments": { "expression": "125 * 24" }
                      }
                    ],
                    "call": { "id": "call-uuid" }
                  }
                }
                """;
        VapiWebhookPayload payload = objectMapper.readValue(json, VapiWebhookPayload.class);
        VapiToolCall toolCall = payload.message().toolCallList().get(0);
        assertEquals("toolu_01DTPAzUm5Gk3zxrpJ969oMF", toolCall.id());
        assertEquals("calculator", toolCall.function().name());
        assertEquals("125 * 24", toolCall.function().arguments().get("expression"));
        assertEquals("call-uuid", payload.message().call().id());
    }

    @Test
    void liveToolCallListAcceptsParametersAndStringArguments() throws Exception {
        String json = """
                {
                  "message": {
                    "type": "tool-calls",
                    "toolCallList": [
                      {
                        "id": "abc123",
                        "name": "calculator",
                        "parameters": { "expression": "125 * 24" }
                      }
                    ]
                  },
                  "call": { "id": "call-uuid" }
                }
                """;
        VapiWebhookPayload payload = objectMapper.readValue(json, VapiWebhookPayload.class);
        VapiFunction function = payload.message().toolCallList().get(0).function();
        assertEquals("calculator", function.name());
        assertEquals("125 * 24", function.arguments().get("expression"));

        String stringArgs = """
                {
                  "id": "call_nested",
                  "type": "function",
                  "function": {
                    "name": "calculator",
                    "arguments": "{\\"expression\\":\\"125 * 24\\"}"
                  }
                }
                """;
        VapiToolCall nested = objectMapper.readValue(stringArgs, VapiToolCall.class);
        assertEquals("calculator", nested.function().name());
        assertEquals("125 * 24", nested.function().arguments().get("expression"));
        assertNotNull(payload.call());
        assertEquals("call-uuid", payload.call().id());
    }
}
