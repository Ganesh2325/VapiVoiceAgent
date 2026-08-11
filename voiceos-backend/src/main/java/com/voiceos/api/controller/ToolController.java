package com.voiceos.api.controller;

import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.ToolRegistry;
import com.voiceos.tool.core.ToolResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for Tool Catalog and direct tool execution.
 */
@RestController
@RequestMapping("/api/v1/tools")
@Tag(name = "Tools", description = "Tool catalog and execution endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ToolController {

    private final ToolRegistry toolRegistry;

    public ToolController(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Operation(summary = "List all registered tools")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listTools() {
        List<Map<String, Object>> tools = toolRegistry.getAllTools().stream()
                .map(t -> Map.<String, Object>of(
                        "name", t.getName(),
                        "description", t.getDescription(),
                        "riskLevel", t.getRiskLevel().name(),
                        "requiresApproval", t.getRiskLevel().requiresHumanApproval(),
                        "timeoutMs", t.getTimeoutMs()
                ))
                .toList();

        return ResponseEntity.ok(tools);
    }

    @Operation(summary = "Execute a specific tool directly")
    @PostMapping("/{name}/execute")
    public ResponseEntity<ToolResult> executeTool(
            @PathVariable String name,
            @RequestBody(required = false) Map<String, Object> params
    ) {
        ToolResult result = toolRegistry.executeTool(name, params != null ? params : Map.of());
        return ResponseEntity.ok(result);
    }
}
