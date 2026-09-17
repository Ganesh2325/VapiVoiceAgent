package com.voiceos.api.controller;

import com.voiceos.domain.entity.User;
import com.voiceos.domain.entity.VoiceSession;
import com.voiceos.domain.repository.ConversationRepository;
import com.voiceos.exception.VoiceOsException;
import com.voiceos.security.AuthenticatedUserService;
import com.voiceos.service.VoiceSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Binds a Vapi callId to the authenticated VoiceOS user.
 * This is the only supported identity mapping for voice tool-calls.
 */
@RestController
@RequestMapping("/api/v1/voice/sessions")
@Tag(name = "Voice Sessions", description = "Bind Vapi calls to authenticated VoiceOS users")
@SecurityRequirement(name = "bearerAuth")
public class VoiceSessionController {

    private final VoiceSessionService voiceSessionService;
    private final AuthenticatedUserService authenticatedUserService;
    private final ConversationRepository conversationRepository;

    public VoiceSessionController(
            VoiceSessionService voiceSessionService,
            AuthenticatedUserService authenticatedUserService,
            ConversationRepository conversationRepository
    ) {
        this.voiceSessionService = voiceSessionService;
        this.authenticatedUserService = authenticatedUserService;
        this.conversationRepository = conversationRepository;
    }

    public record BindRequest(String callId, UUID conversationId) {}

    @PostMapping
    @Operation(summary = "Bind the current JWT user to a Vapi callId")
    public ResponseEntity<Map<String, Object>> bind(@RequestBody BindRequest request) {
        User user = authenticatedUserService.requireUser();
        if (request == null || request.callId() == null || request.callId().isBlank()) {
            throw VoiceOsException.badRequest("callId is required");
        }
        UUID conversationId = request.conversationId();
        if (conversationId != null) {
            conversationRepository.findByIdAndUserId(conversationId, user.getId())
                    .orElseThrow(() -> VoiceOsException.forbidden(
                            "Conversation does not belong to the authenticated user"));
        }
        VoiceSession session = voiceSessionService.bind(user, request.callId(), conversationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "sessionId", session.getId().toString(),
                "callId", session.getCallId(),
                "userId", user.getId().toString(),
                "status", session.getStatus().name(),
                "expiresAt", session.getExpiresAt().toString()
        ));
    }
}
