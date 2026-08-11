package com.voiceos.api.controller;

import com.voiceos.ai.LLMProvider;
import com.voiceos.voice.VoiceProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Health and system status controller.
 * /actuator/health provides Spring Boot's built-in health checks.
 * This endpoint provides VoiceOS-specific provider status.
 */
@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System", description = "System health and provider status")
public class SystemController {

    private final LLMProvider llmProvider;
    private final VoiceProvider voiceProvider;

    public SystemController(LLMProvider llmProvider, VoiceProvider voiceProvider) {
        this.llmProvider = llmProvider;
        this.voiceProvider = voiceProvider;
    }

    @Operation(
        summary = "Get system status",
        description = "Returns the status of VoiceOS providers (LLM, voice, etc.).",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString(),
                "version", "1.0.0",
                "providers", Map.of(
                        "llm", Map.of(
                                "name", llmProvider.getProviderName(),
                                "available", llmProvider.isAvailable()
                        ),
                        "voice", Map.of(
                                "name", voiceProvider.getProviderName(),
                                "available", voiceProvider.isAvailable()
                        )
                )
        ));
    }
}
