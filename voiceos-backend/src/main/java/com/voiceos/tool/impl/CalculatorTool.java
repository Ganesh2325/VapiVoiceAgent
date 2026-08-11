package com.voiceos.tool.impl;

import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Deterministic Calculator tool for budgeting and arithmetic.
 * Risk Level: LOW (safe, read-only calculation).
 */
@Component
public class CalculatorTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(CalculatorTool.class);

    @Override
    public String getName() {
        return "calculator";
    }

    @Override
    public String getDescription() {
        return "Performs mathematical and financial calculations (e.g. total trip costs, currency conversion, budget estimates).";
    }

    @Override
    public ToolRiskLevel getRiskLevel() {
        return ToolRiskLevel.LOW;
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
        long startMs = System.currentTimeMillis();
        try {
            String expression = (String) params.getOrDefault("expression", "");
            String operation = (String) params.getOrDefault("operation", "evaluate");

            log.info("Calculator executing: expr='{}', op='{}'", expression, operation);

            double result = evaluateExpression(expression);
            long latency = System.currentTimeMillis() - startMs;

            return ToolResult.success(
                    Map.of("expression", expression, "result", result),
                    String.format(java.util.Locale.US, "%.2f", result),
                    latency
            );
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startMs;
            return ToolResult.failure("Calculation error: " + e.getMessage(), latency);
        }
    }

    private double evaluateExpression(String expr) {
        if (expr == null || expr.isBlank()) return 0.0;
        String clean = expr.replaceAll("[^0-9.+\\-*/]", " ").trim();
        if (clean.isBlank()) return 0.0;

        try {
            // Handle multiplication
            if (clean.contains("*")) {
                String[] parts = clean.split("\\*");
                double prod = 1.0;
                for (String p : parts) {
                    if (!p.trim().isEmpty()) {
                        prod *= Double.parseDouble(p.trim());
                    }
                }
                return prod;
            }

            // Handle addition
            String[] tokens = clean.split("\\+");
            double sum = 0.0;
            for (String t : tokens) {
                String trimmed = t.trim();
                if (!trimmed.isEmpty()) {
                    sum += Double.parseDouble(trimmed);
                }
            }
            return sum;
        } catch (Exception ignored) {
            return 0.0;
        }
    }
}
