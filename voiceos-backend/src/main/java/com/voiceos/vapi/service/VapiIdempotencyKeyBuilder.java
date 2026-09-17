package com.voiceos.vapi.service;

import com.voiceos.exception.VoiceOsException;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiCall;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiMessage;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiToolCall;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiWebhookPayload;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Builds a stable idempotency identity from the actual Vapi event model.
 *
 * <p>Does not use wall-clock time. Prefers identifiers Vapi already supplies:
 * optional {@code message.id}, {@code toolCall.id}, {@code call.id}, event type,
 * status, and payload timestamp.
 */
@Component
public class VapiIdempotencyKeyBuilder {

    public String build(VapiWebhookPayload payload) {
        if (payload == null || payload.message() == null) {
            throw VoiceOsException.badRequest("Webhook event is missing a message");
        }

        VapiMessage message = payload.message();
        if (message.id() != null && !message.id().isBlank()) {
            return "vapi:msg:" + message.id().trim();
        }

        String callId = resolveCallId(payload);
        String eventType = message.type() != null ? message.type().toLowerCase(Locale.ROOT) : "unknown";

        String toolCallIds = joinToolCallIds(message);
        if (!toolCallIds.isBlank()) {
            if (callId != null) {
                return "vapi:call:" + callId + ":tools:" + toolCallIds;
            }
            return "vapi:tools:" + toolCallIds;
        }

        if ("transcript".equals(eventType) && callId != null) {
            String digest = sha256Hex(nullToEmpty(message.role()) + "|" + nullToEmpty(message.transcript()));
            return "vapi:call:" + callId + ":transcript:" + digest;
        }

        if (callId != null && message.status() != null && !message.status().isBlank()) {
            return "vapi:call:" + callId + ":" + eventType + ":" + message.status();
        }

        if (callId != null && payload.timestamp() != null && !payload.timestamp().isBlank()) {
            return "vapi:call:" + callId + ":" + eventType + ":" + payload.timestamp();
        }

        if (callId != null) {
            return "vapi:call:" + callId + ":" + eventType;
        }

        throw VoiceOsException.badRequest(
                "Webhook event is missing a stable identity (message.id, call.id, or toolCall.id)");
    }

    public String resolveCallId(VapiWebhookPayload payload) {
        if (payload.call() != null && payload.call().id() != null && !payload.call().id().isBlank()) {
            return payload.call().id();
        }
        VapiCall nested = payload.message() != null ? payload.message().call() : null;
        if (nested != null && nested.id() != null && !nested.id().isBlank()) {
            return nested.id();
        }
        return null;
    }

    public String resolveEventId(VapiWebhookPayload payload) {
        if (payload != null && payload.message() != null && payload.message().id() != null) {
            return payload.message().id();
        }
        return null;
    }

    private static String joinToolCallIds(VapiMessage message) {
        List<VapiToolCall> calls = message.toolCalls() != null ? message.toolCalls() : message.toolCallList();
        if (calls == null || calls.isEmpty()) {
            return "";
        }
        return calls.stream()
                .map(VapiToolCall::id)
                .filter(id -> id != null && !id.isBlank())
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(","));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required for idempotency keys", e);
        }
    }
}
