package com.voiceos.tool.impl;

import com.voiceos.provider.ProviderMode;
import com.voiceos.provider.ProviderRequest;
import com.voiceos.provider.ProviderResult;
import com.voiceos.provider.ProviderResults;
import com.voiceos.provider.calculator.CalculatorProvider;
import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic Calculator tool for arithmetic.
 * Risk Level: LOW. Provider: REAL local arithmetic.
 */
@Component
public class CalculatorTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(CalculatorTool.class);
    private final CalculatorProvider provider;

    public CalculatorTool(CalculatorProvider provider) {
        this.provider = Objects.requireNonNull(provider, "CalculatorProvider is required");
    }

    @Override
    public String getName() {
        return "calculator";
    }

    @Override
    public String getDescription() {
        return "Performs mathematical calculations. REAL local arithmetic; result is computed, not hardcoded.";
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

    @Override
    public long getTimeoutMs() {
        return 5_000L;
    }

    @Override
    public void validateInput(Map<String, Object> params) {
        Tool.super.validateInput(params);
        if (!params.containsKey("expression") && !params.containsKey("operation")) {
            throw new IllegalArgumentException("Calculator requires either 'expression' or 'operation' parameter.");
        }
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        Map<String, Object> safe = new HashMap<>(params != null ? params : Map.of());
        safe.remove("provider");
        safe.remove("providerName");
        safe.remove("providerClass");
        String expression = String.valueOf(safe.getOrDefault("expression", ""));
        log.info("Calculator executing: expr='{}', provider={} mode={}",
                expression, provider.getName(), provider.getMode());
        ProviderResult result = provider.execute(new ProviderRequest("evaluate", safe, getTimeoutMs()));
        return ProviderResults.toToolResult(result);
    }
}
