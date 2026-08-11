package com.voiceos.voice;

/**
 * Voice Provider abstraction.
 *
 * <p>All voice interactions go through this interface.
 * This decouples the platform from any specific voice provider.
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@code LiveKitVoiceProvider} — open-source, self-hosted (MVP)</li>
 *   <li>{@code VapiVoiceProvider} — optional commercial integration</li>
 *   <li>{@code StubVoiceProvider} — development stub</li>
 * </ul>
 */
public interface VoiceProvider {

    /**
     * Creates a new voice session/room for a conversation.
     *
     * @param conversationId the conversation ID
     * @param userId the user ID
     * @return the voice session details
     */
    VoiceSession createSession(String conversationId, String userId);

    /**
     * Terminates an active voice session.
     *
     * @param sessionId the voice session ID to terminate
     */
    void terminateSession(String sessionId);

    /**
     * Generates a participant token for joining a voice session.
     *
     * @param sessionId the session to join
     * @param userId the participant user ID
     * @return JWT/token for the participant
     */
    String generateParticipantToken(String sessionId, String userId);

    /**
     * @return the provider name (e.g. "livekit", "vapi", "stub")
     */
    String getProviderName();

    /**
     * @return whether this provider is currently available
     */
    boolean isAvailable();

    /**
     * Voice session details returned after creating a session.
     *
     * @param sessionId     unique session identifier
     * @param serverUrl     WebSocket URL to connect to
     * @param token         participant token for the creating user
     * @param providerName  which provider is hosting this session
     */
    record VoiceSession(
            String sessionId,
            String serverUrl,
            String token,
            String providerName
    ) {}
}
