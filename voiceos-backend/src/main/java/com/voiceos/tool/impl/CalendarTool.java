package com.voiceos.tool.impl;

import com.voiceos.provider.ProviderMode;
import com.voiceos.provider.ProviderRequest;
import com.voiceos.provider.ProviderResults;
import com.voiceos.provider.calendar.CalendarProvider;
import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * Calendar Management Tool. Phase 4 uses a MOCK calendar provider.
 */
@Component
public class CalendarTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(CalendarTool.class);
    private final CalendarProvider provider;

    public CalendarTool(CalendarProvider provider) {
        this.provider = Objects.requireNonNull(provider, "CalendarProvider is required");
    }

    @Override
    public String getName() {
        return "calendar";
    }

    @Override
    public String getDescription() {
        return "[MOCK] Simulated calendar read/write. Does not access a real calendar.";
    }

    @Override
    public ToolRiskLevel getRiskLevel() {
        return ToolRiskLevel.MEDIUM;
    }

    @Override
    public String getProviderName() {
        return provider.getName();
    }

    @Override
    public ProviderMode getProviderMode() {
        return provider.getMode();
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        String action = params != null ? String.valueOf(params.getOrDefault("action", "check_schedule")) : "check_schedule";
        log.info("CalendarTool executing via {} mode={} action={}", provider.getName(), provider.getMode(), action);
        String operation = action.toLowerCase().contains("create") ? "write" : "read";
        return ProviderResults.toToolResult(provider.execute(new ProviderRequest(operation, params, getTimeoutMs())));
    }
}
