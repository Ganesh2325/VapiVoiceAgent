package com.voiceos.tool.impl;

import com.voiceos.provider.ProviderMode;
import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.ToolResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tool that PolicyEngine must DENY. Execute is instrumented so tests can prove it never ran.
 */
@Component
public class ForbiddenTool implements Tool {

    private final AtomicInteger executions = new AtomicInteger();

    @Override
    public String getName() {
        return "forbidden";
    }

    @Override
    public String getDescription() {
        return "[TEST] Policy-denied tool. Must never execute.";
    }

    @Override
    public ToolRiskLevel getRiskLevel() {
        return ToolRiskLevel.LOW;
    }

    @Override
    public String getProviderName() {
        return "none";
    }

    @Override
    public ProviderMode getProviderMode() {
        return ProviderMode.MOCK;
    }

    public int executionCount() {
        return executions.get();
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        executions.incrementAndGet();
        return ToolResult.success("should-not-run", 0);
    }
}
