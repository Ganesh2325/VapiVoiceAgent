package com.voiceos.api.controller;

import com.voiceos.action.engine.ActionEngine;
import com.voiceos.action.model.ActionCommand;
import com.voiceos.action.model.ActionExecutionResult;
import com.voiceos.domain.entity.User;
import com.voiceos.security.AuthenticatedUserService;
import com.voiceos.security.VoiceOsRequestContext;
import com.voiceos.tool.core.ToolRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * Tool catalog. Execution is routed through ActionEngine, not ToolRegistry directly.
 */
@RestController
@RequestMapping("/api/v1/tools")
@Tag(name = "Tools", description = "Tool catalog and execution endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ToolController {

    private final ToolRegistry toolRegistry;
    private final AuthenticatedUserService authenticatedUserService;
    private final ActionEngine actionEngine;

    public ToolController(
            ToolRegistry toolRegistry,
            AuthenticatedUserService authenticatedUserService,
            ActionEngine actionEngine
    ) {
        this.toolRegistry = toolRegistry;
        this.authenticatedUserService = authenticatedUserService;
        this.actionEngine = actionEngine;
    }

    @Operation(summary = "List all registered tools")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listTools() {
        authenticatedUserService.requireUser();
        List<Map<String, Object>> tools = toolRegistry.getAllTools().stream()
                .map(t -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("name", t.getName());
                    row.put("description", t.getDescription());
                    row.put("riskLevel", t.getRiskLevel().name());
                    row.put("requiresApproval", t.getRiskLevel().requiresHumanApproval());
                    row.put("timeoutMs", t.getTimeoutMs());
                    row.put("provider", t.getProviderName());
                    row.put("providerMode", t.getProviderMode().name());
                    return row;
                })
                .toList();
        return ResponseEntity.ok(tools);
    }

    @Operation(summary = "Execute a named tool through ActionEngine")
    @PostMapping("/{name}/execute")
    public ResponseEntity<Map<String, Object>> executeTool(
            @PathVariable String name,
            @RequestBody(required = false) Map<String, Object> params
    ) {
        User user = authenticatedUserService.requireUser();
        Map<String, Object> safeParams = new HashMap<>(params != null ? params : Map.of());
        safeParams.remove("provider");
        safeParams.remove("providerName");
        safeParams.remove("providerClass");
        safeParams.remove("agent");
        safeParams.remove("agentName");
        safeParams.put("userId", user.getId().toString());

        ActionCommand command = new ActionCommand(
                user.getId(),
                null,
                null,
                VoiceOsRequestContext.currentRequestId(),
                null,
                null,
                "HTTP",
                name,
                name,
                safeParams
        );
        ActionExecutionResult result = actionEngine.execute(command);

        Map<String, Object> body = new HashMap<>();
        body.put("success", result.success());
        body.put("data", result.result() != null ? Map.of("result", result.result()) : Map.of());
        body.put("rawOutput", result.result());
        body.put("errorMessage", result.error());
        body.put("durationMs", result.totalLatencyMs());
        body.put("actionId", result.actionId());
        body.put("status", result.status());
        body.put("agent", result.agent());
        body.put("tool", result.tool());
        body.put("provider", result.provider());
        body.put("providerMode", result.providerMode());
        return ResponseEntity.ok(body);
    }
}
