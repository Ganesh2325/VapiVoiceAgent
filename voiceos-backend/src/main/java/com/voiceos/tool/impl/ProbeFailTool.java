package com.voiceos.tool.impl;

import com.voiceos.provider.ProviderMode;
import com.voiceos.provider.ProviderRequest;
import com.voiceos.provider.ProviderResults;
import com.voiceos.provider.probe.FailingProbeProvider;
import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.ToolResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Controlled failing tool for pipeline tests. Policy ALLOW, execution always FAILURE.
 */
@Component
public class ProbeFailTool implements Tool {

    private final AtomicInteger executions = new AtomicInteger();
    private final FailingProbeProvider provider;

    public ProbeFailTool(FailingProbeProvider provider) {
        this.provider = Objects.requireNonNull(provider, "FailingProbeProvider is required");
    }

    @Override
    public String getName() {
        return "probe_fail";
    }

    @Override
    public String getDescription() {
        return "[TEST] Always fails. Used to prove provider/tool failure yields FAILED, not COMPLETED.";
    }

    @Override
    public ToolRiskLevel getRiskLevel() {
        return ToolRiskLevel.LOW;
    }

    @Override
    public String getProviderName() {
        return provider.getName();
    }

    @Override
    public ProviderMode getProviderMode() {
        return provider.getMode();
    }

    public int executionCount() {
        return executions.get();
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        executions.incrementAndGet();
        return ProviderResults.toToolResult(provider.execute(new ProviderRequest("fail", params, getTimeoutMs())));
    }
}
