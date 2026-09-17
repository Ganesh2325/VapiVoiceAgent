package com.voiceos.api.controller;

import com.voiceos.action.audit.ActionTimelineDto;
import com.voiceos.action.engine.ActionEngine;
import com.voiceos.action.model.Action;
import com.voiceos.action.model.ActionCommand;
import com.voiceos.action.model.ActionExecutionResult;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Observability API for persisted actions, steps, and audit events.
 * Execution remains ActionEngine via agent/tool endpoints.
 */
@RestController
@RequestMapping("/api/v1/actions")
@Tag(name = "Actions", description = "Canonical action observability")
@SecurityRequirement(name = "bearerAuth")
public class ActionController {

    private final ActionEngine actionEngine;
    private final AuthenticatedUserService authenticatedUserService;
    private final ConversationRepository conversationRepository;

    public ActionController(
            ActionEngine actionEngine,
            AuthenticatedUserService authenticatedUserService,
            ConversationRepository conversationRepository
    ) {
        this.actionEngine = actionEngine;
        this.authenticatedUserService = authenticatedUserService;
        this.conversationRepository = conversationRepository;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        User user = authenticatedUserService.requireUser();
        List<Map<String, Object>> actions = actionEngine.listActionsForUser(user.getId()).stream()
                .map(ActionController::toView)
                .toList();
        return ResponseEntity.ok(actions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable UUID id) {
        User user = authenticatedUserService.requireUser();
        Action action = actionEngine.getActionForUser(id, user.getId())
                .orElseThrow(() -> VoiceOsException.notFound("Action", id));
        return ResponseEntity.ok(toView(action));
    }

    @GetMapping("/{id}/timeline")
    @Operation(summary = "Owner-scoped action timeline with ordered steps and audit events")
    public ResponseEntity<ActionTimelineDto> timeline(@PathVariable UUID id) {
        User user = authenticatedUserService.requireUser();
        ActionTimelineDto timeline = actionEngine.timelineForUser(id, user.getId())
                .orElseThrow(() -> VoiceOsException.notFound("Action", id));
        return ResponseEntity.ok(timeline);
    }

    @GetMapping("/{id}/steps")
    public ResponseEntity<List<ActionTimelineDto.StepView>> steps(@PathVariable UUID id) {
        User user = authenticatedUserService.requireUser();
        ActionTimelineDto timeline = actionEngine.timelineForUser(id, user.getId())
                .orElseThrow(() -> VoiceOsException.notFound("Action", id));
        return ResponseEntity.ok(timeline.steps());
    }

    @GetMapping("/{id}/events")
    public ResponseEntity<List<ActionTimelineDto.EventView>> events(@PathVariable UUID id) {
        User user = authenticatedUserService.requireUser();
        ActionTimelineDto timeline = actionEngine.timelineForUser(id, user.getId())
                .orElseThrow(() -> VoiceOsException.notFound("Action", id));
        return ResponseEntity.ok(timeline.events());
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancel(@PathVariable UUID id) {
        User user = authenticatedUserService.requireUser();
        try {
            Action action = actionEngine.cancel(id, user.getId());
            return ResponseEntity.ok(toView(action));
        } catch (IllegalArgumentException e) {
            throw VoiceOsException.notFound("Action", id);
        }
    }

    public record ExecuteRequest(
            UUID conversationId,
            @NotBlank String input,
            String tool
    ) {}

    @PostMapping
    @Operation(summary = "Execute via canonical ActionEngine (same pipeline as agent/execute)")
    public ResponseEntity<ActionExecutionResult> execute(@RequestBody ExecuteRequest request) {
        User user = authenticatedUserService.requireUser();
        UUID convId = request.conversationId();
        if (convId != null) {
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
                request.tool(),
                Map.of()
        );
        return ResponseEntity.ok(actionEngine.execute(command));
    }

    static Map<String, Object> toView(Action action) {
        Map<String, Object> view = new HashMap<>();
        view.put("actionId", action.getId());
        view.put("status", action.getStatus() != null ? action.getStatus().name() : null);
        view.put("agent", action.getTargetAgent());
        view.put("tool", action.getToolName());
        view.put("provider", action.getTargetProvider());
        view.put("providerMode", action.getProviderMode());
        view.put("result", action.getResult());
        view.put("error", action.getErrorMessage());
        view.put("durationMs", action.getDurationMs());
        view.put("conversationId", action.getConversationId());
        view.put("callId", action.getCallId());
        view.put("verificationPassed", action.getVerificationPassed());
        view.put("createdAt", action.getCreatedAt());
        view.put("completedAt", action.getCompletedAt());
        if (action.getPayload() != null) {
            view.put("agentOutcome", action.getPayload().get("agentOutcome"));
            view.put("missingFields", action.getPayload().get("missingFields"));
        }
        return view;
    }
}
