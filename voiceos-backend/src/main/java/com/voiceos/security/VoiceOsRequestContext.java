package com.voiceos.security;

import java.util.UUID;

/**
 * Minimal per-request correlation context for VoiceOS.
 *
 * <p>Establishes the identity contract future agent execution will require:
 * {@code requestId}, {@code userId}, {@code conversationId}, {@code callId}, {@code eventId}.
 * Stored in a ThreadLocal and cleared at the end of each HTTP request.
 */
public final class VoiceOsRequestContext {

    private static final ThreadLocal<VoiceOsRequestContext> HOLDER = new ThreadLocal<>();

    private final String requestId;
    private final UUID userId;
    private final UUID conversationId;
    private final String callId;
    private final String eventId;

    public VoiceOsRequestContext(String requestId, UUID userId, UUID conversationId, String callId, String eventId) {
        this.requestId = requestId;
        this.userId = userId;
        this.conversationId = conversationId;
        this.callId = callId;
        this.eventId = eventId;
    }

    public static void set(VoiceOsRequestContext context) {
        HOLDER.set(context);
    }

    public static VoiceOsRequestContext current() {
        return HOLDER.get();
    }

    public static String currentRequestId() {
        VoiceOsRequestContext ctx = HOLDER.get();
        return ctx != null ? ctx.requestId() : null;
    }

    public static void clear() {
        HOLDER.remove();
    }

    public VoiceOsRequestContext withUserId(UUID newUserId) {
        return new VoiceOsRequestContext(requestId, newUserId, conversationId, callId, eventId);
    }

    public VoiceOsRequestContext withCall(String newCallId, String newEventId) {
        return new VoiceOsRequestContext(requestId, userId, conversationId, newCallId, newEventId);
    }

    public VoiceOsRequestContext withConversation(UUID newConversationId) {
        return new VoiceOsRequestContext(requestId, userId, newConversationId, callId, eventId);
    }

    public String requestId() {
        return requestId;
    }

    public UUID userId() {
        return userId;
    }

    public UUID conversationId() {
        return conversationId;
    }

    public String callId() {
        return callId;
    }

    public String eventId() {
        return eventId;
    }
}
