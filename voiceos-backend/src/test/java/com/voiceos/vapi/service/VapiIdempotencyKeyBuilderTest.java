package com.voiceos.vapi.service;

import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiCall;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiFunction;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiMessage;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiToolCall;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiWebhookPayload;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class VapiIdempotencyKeyBuilderTest {

    private final VapiIdempotencyKeyBuilder builder = new VapiIdempotencyKeyBuilder();

    @Test
    void messageIdIsPreferred() {
        VapiMessage message = new VapiMessage(
                "status-update", null, null, null, null, "ended", null, null, null, null, null, null, null, null, null, null, "evt-1"
        );
        String key = builder.build(new VapiWebhookPayload(message, new VapiCall("call-1", null, null, null, null, null, null, null), "t"));
        assertEquals("vapi:msg:evt-1", key);
        assertEquals(key, builder.build(new VapiWebhookPayload(message, new VapiCall("call-1", null, null, null, null, null, null, null), "t")));
    }

    @Test
    void toolCallIdsAreStableAndIndependentOfClock() {
        VapiToolCall call = new VapiToolCall("tc-9", "function", new VapiFunction("calculator", Map.of("expression", "1+1")));
        VapiMessage message = new VapiMessage(
                "tool-calls", null, List.of(call), null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        VapiWebhookPayload payload = new VapiWebhookPayload(
                message, new VapiCall("call-abc", null, null, null, null, null, null, null), "2026-09-17T00:00:00Z");
        String first = builder.build(payload);
        String second = builder.build(payload);
        assertEquals(first, second);
        assertEquals("vapi:call:call-abc:tools:tc-9", first);
        assertNotEquals(true, first.contains(String.valueOf(System.currentTimeMillis()).substring(0, 8)));
    }
}
