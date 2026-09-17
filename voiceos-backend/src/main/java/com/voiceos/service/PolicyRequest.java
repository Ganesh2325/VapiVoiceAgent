package com.voiceos.service;

import com.voiceos.tool.core.Tool.ToolRiskLevel;

import java.util.Map;
import java.util.UUID;

/**
 * Structured input to {@link PolicyEngine}. Decisions are not based on free-text keywords.
 */
public record PolicyRequest(
        UUID userId,
        String actionType,
        String toolName,
        ToolRiskLevel riskLevel,
        String provider,
        Map<String, Object> context
) {}
