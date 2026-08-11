package com.voiceos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voiceos.config.VoiceOsProperties;
import com.voiceos.tool.core.ToolRegistry;
import com.voiceos.tool.impl.CalculatorTool;
import com.voiceos.tool.impl.SearchTool;
import com.voiceos.vapi.dto.VapiWebhookDTOs.*;
import com.voiceos.vapi.service.VapiEventProcessor;
import com.voiceos.vapi.service.VapiToolService;
import com.voiceos.vapi.service.VapiWebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VapiWebhookServiceTest {

    private VapiWebhookService vapiWebhookService;
    private ToolRegistry toolRegistry;

    @BeforeEach
    void setUp() {
        CalculatorTool calcTool = new CalculatorTool();
        SearchTool searchTool = new SearchTool();
        toolRegistry = new ToolRegistry(List.of(calcTool, searchTool));

        VapiToolService toolService = new VapiToolService(
                toolRegistry,
                null,
                null,
                null
        );

        VapiEventProcessor eventProcessor = new VapiEventProcessor(
                null,
                null,
                null,
                null
        );

        VoiceOsProperties props = new VoiceOsProperties(
                null, null,
                new VoiceOsProperties.VoiceProperties("vapi", new VoiceOsProperties.VapiProperties("test_api_key", "test_ast", "secret123", "pk_123", "https://api.vapi.ai")),
                null,
                new VoiceOsProperties.WebhooksProperties("secret123", null, null),
                null, null, null
        );

        vapiWebhookService = new VapiWebhookService(
                toolService,
                eventProcessor,
                null,
                props,
                new ObjectMapper()
        );
    }

    @Test
    void testSecretValidation() {
        assertTrue(vapiWebhookService.validateSecret("secret123"));
        assertFalse(vapiWebhookService.validateSecret("wrong_secret"));
    }

    @Test
    void testToolCallExecution() {
        VapiToolCall toolCall = new VapiToolCall(
                "call-1",
                "function",
                new VapiFunction("calculator", Map.of("expression", "25 * 4", "operation", "sum"))
        );

        VapiMessage msg = new VapiMessage(
                "tool-calls",
                null,
                List.of(toolCall),
                null, null, null, null, null, null, null, null, null, null, null, null, null
        );

        VapiWebhookPayload payload = new VapiWebhookPayload(msg, new VapiCall("call-abc", "org-1", "ast-1", null, "webCall", "in-progress", null, null), "2026-08-11T00:00:00Z");

        Object result = vapiWebhookService.processWebhook(payload, "{}");
        assertTrue(result instanceof VapiToolCallResponse);

        VapiToolCallResponse response = (VapiToolCallResponse) result;
        assertEquals(1, response.results().size());
        assertEquals("100.00", response.results().get(0).result());
    }

    @Test
    void testAssistantRequestResponse() {
        VapiMessage msg = new VapiMessage(
                "assistant-request",
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );

        VapiWebhookPayload payload = new VapiWebhookPayload(msg, null, null);
        Object result = vapiWebhookService.processWebhook(payload, "{}");

        assertTrue(result instanceof VapiAssistantResponse);
        VapiAssistantResponse res = (VapiAssistantResponse) result;
        assertNotNull(res.assistant().get("firstMessage"));
    }
}
