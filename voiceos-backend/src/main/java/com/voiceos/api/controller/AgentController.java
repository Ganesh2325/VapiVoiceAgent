package com.voiceos.api.controller;

import com.voiceos.action.engine.ActionEngine;
import com.voiceos.action.model.ActionCommand;
import com.voiceos.action.model.ActionExecutionResult;
import com.voiceos.agent.core.AgentRegistry;
import com.voiceos.domain.entity.Conversation;
import com.voiceos.domain.entity.User;
import com.voiceos.domain.repository.ConversationRepository;
import com.voiceos.exception.VoiceOsException;
import com.voiceos.security.AuthenticatedUserService;
import com.voiceos.security.VoiceOsRequestContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API for Agent catalog and execution through the canonical ActionEngine.
 */
@RestController
@RequestMapping("/api/v1/agent")
@Tag(name = "Agent Execution", description = "Execute instructions via ActionEngine")
@SecurityRequirement(name = "bearerAuth")
public class AgentController {

    private final ActionEngine actionEngine;
    private final AgentRegistry agentRegistry;
    private final ConversationRepository conversationRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public AgentController(
            ActionEngine actionEngine,
            AgentRegistry agentRegistry,
            ConversationRepository conversationRepository,
            AuthenticatedUserService authenticatedUserService
    ) {
        this.actionEngine = actionEngine;
        this.agentRegistry = agentRegistry;
        this.conversationRepository = conversationRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    public record ExecuteRequest(
            UUID conversationId,
            @NotBlank(message = "Input instruction cannot be blank")
            String input
    ) {}

    @Operation(
            summary = "Execute agent instruction",
            description = "Processes user instruction through ActionEngine → PolicyEngine → Agent → Tool → Provider → Verification."
    )
    @PostMapping("/execute")
    public ResponseEntity<ActionExecutionResult> execute(@RequestBody ExecuteRequest request) {
        User user = authenticatedUserService.requireUser();

        UUID convId = request.conversationId();
        if (convId == null) {
            Conversation conv = new Conversation(user, request.input());
            conv = conversationRepository.save(conv);
            convId = conv.getId();
        } else {
            conversationRepository.findByIdAndUserId(convId, user.getId())
                    .orElseThrow(() -> VoiceOsException.forbidden(
                            "Conversation does not belong to the authenticated user"));
        }

        ActionCommand command = new ActionCommand(
                user.getId(),
                convId,
                null,
                VoiceOsRequestContext.currentRequestId(),
                null,
                null,
                "HTTP",
                request.input(),
                null,
                Map.of()
        );
        return ResponseEntity.ok(actionEngine.execute(command));
    }

    @Operation(summary = "List all available AI agents")
    @GetMapping("/catalog")
    public ResponseEntity<List<Map<String, Object>>> listAgents() {
        authenticatedUserService.requireUser();
        return ResponseEntity.ok(agentRegistry.catalog());
    }
}
