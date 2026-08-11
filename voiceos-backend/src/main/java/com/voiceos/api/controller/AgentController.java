package com.voiceos.api.controller;

import com.voiceos.agent.core.Agent;
import com.voiceos.agent.core.AgentRegistry;
import com.voiceos.domain.entity.Conversation;
import com.voiceos.domain.entity.User;
import com.voiceos.domain.repository.ConversationRepository;
import com.voiceos.domain.repository.UserRepository;
import com.voiceos.exception.VoiceOsException;
import com.voiceos.orchestrator.OrchestratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API for Agent Execution and Agent Catalog.
 */
@RestController
@RequestMapping("/api/v1/agent")
@Tag(name = "Agent Execution", description = "Execute instructions via multi-agent orchestrator")
@SecurityRequirement(name = "bearerAuth")
public class AgentController {

    private final OrchestratorService orchestratorService;
    private final AgentRegistry agentRegistry;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    public AgentController(
            OrchestratorService orchestratorService,
            AgentRegistry agentRegistry,
            ConversationRepository conversationRepository,
            UserRepository userRepository
    ) {
        this.orchestratorService = orchestratorService;
        this.agentRegistry = agentRegistry;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
    }

    public record ExecuteRequest(
            UUID conversationId,
            @NotBlank(message = "Input instruction cannot be blank")
            String input
    ) {}

    @Operation(
        summary = "Execute agent instruction",
        description = "Processes user instruction through the Orchestrator, delegating to specialized agents and executing tools."
    )
    @PostMapping("/execute")
    public ResponseEntity<OrchestratorService.OrchestrationResult> execute(
            @RequestBody ExecuteRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getUser(userDetails);

        // Auto-create conversation if none provided
        UUID convId = request.conversationId();
        if (convId == null) {
            Conversation conv = new Conversation(user, request.input());
            conv = conversationRepository.save(conv);
            convId = conv.getId();
        }

        OrchestratorService.OrchestrationResult result = orchestratorService.processUserRequest(
                convId, user.getId(), request.input()
        );

        return ResponseEntity.ok(result);
    }

    @Operation(summary = "List all available AI agents")
    @GetMapping("/catalog")
    public ResponseEntity<List<Map<String, Object>>> listAgents() {
        List<Map<String, Object>> catalog = agentRegistry.getAllAgents().stream()
                .map(a -> Map.<String, Object>of(
                        "name", a.getName(),
                        "description", a.getDescription(),
                        "priority", a.getPriority(),
                        "toolsCount", a.getTools().size()
                ))
                .toList();

        return ResponseEntity.ok(catalog);
    }

    private User getUser(UserDetails userDetails) {
        if (userDetails == null) {
            return userRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> VoiceOsException.badRequest("User not found"));
        }
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> VoiceOsException.notFound("User", userDetails.getUsername()));
    }
}
