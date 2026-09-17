package com.voiceos.action.model;

import java.util.Map;
import java.util.UUID;

/**
 * Canonical command into {@code ActionEngine}.
 */
public record ActionCommand(
        UUID userId,
        UUID conversationId,
        String callId,
        String requestId,
        String eventId,
        String idempotencyKey,
        String source,
        String requestedOperation,
        String requestedTool,
        Map<String, Object> params
) {
    public static ActionCommand of(UUID userId, String operation, String tool, Map<String, Object> params) {
        return new ActionCommand(userId, null, null, null, null, null, "HTTP", operation, tool, params);
    }
}
