package com.voiceos.provider.calculator;

import com.voiceos.provider.ProviderAvailability;
import com.voiceos.provider.ProviderBinding;
import com.voiceos.provider.ProviderCapability;
import com.voiceos.provider.ProviderErrorCode;
import com.voiceos.provider.ProviderMode;
import com.voiceos.provider.ProviderRequest;
import com.voiceos.provider.ProviderResult;
import com.voiceos.provider.ProviderSupport;
import com.voiceos.provider.ServiceProvider;
import com.voiceos.config.VoiceOsProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * REAL local arithmetic. No external network. Result is computed, not hardcoded.
 * MOCK mode is rejected rather than silently simulating arithmetic.
 */
@Component
public class RealLocalCalculatorProvider implements CalculatorProvider, ServiceProvider {

    private final ProviderBinding binding;

    public RealLocalCalculatorProvider() {
        this.binding = ProviderBinding.realDefault();
    }

    @Autowired
    public RealLocalCalculatorProvider(@Autowired(required = false) VoiceOsProperties properties) {
        this.binding = properties != null && properties.providers() != null
                ? properties.providers().calculator()
                : ProviderBinding.realDefault();
    }

    @Override
    public String getName() {
        return "RealLocalCalculatorProvider";
    }

    @Override
    public String getDescription() {
        return "REAL local arithmetic. Computes the submitted expression.";
    }

    @Override
    public ProviderMode getMode() {
        return ProviderMode.REAL;
    }

    @Override
    public ProviderMode mode() {
        return getMode();
    }

    @Override
    public Set<ProviderCapability> capabilities() {
        return Set.of(ProviderCapability.CALCULATOR);
    }

    @Override
    public ProviderAvailability availability() {
        if (binding != null && binding.modeOr(ProviderMode.REAL) == ProviderMode.MOCK) {
            return ProviderAvailability.MISCONFIGURED;
        }
        return ProviderSupport.availability(binding, true);
    }

    @Override
    public boolean supportsOperation(String operation) {
        if (operation == null) {
            return false;
        }
        String normalized = operation.toLowerCase(Locale.ROOT);
        return normalized.contains("calc") || normalized.contains("evaluate") || normalized.equals("execute");
    }

    @Override
    public ProviderResult execute(ProviderRequest request) {
        long startMs = System.currentTimeMillis();
        ProviderResult blocked = ProviderSupport.rejectIfUnavailable(this, request, startMs);
        if (blocked != null) {
            return blocked;
        }
        String expression = request.paramAsString("expression");
        if (expression == null || expression.isBlank()) {
            return ProviderResult.failure(getName(), getMode(), request.operation(),
                    ProviderErrorCode.INVALID_REQUEST, "Calculator requires an expression", elapsed(startMs));
        }
        return evaluate(expression);
    }

    @Override
    public ProviderResult evaluate(String expression) {
        long startMs = System.currentTimeMillis();
        if (expression == null || expression.isBlank()) {
            return ProviderResult.failure(getName(), getMode(), "evaluate",
                    ProviderErrorCode.INVALID_REQUEST, "Calculator requires an expression", elapsed(startMs));
        }
        String clean = expression.replaceAll("[^0-9.+\\-*/]", " ").trim();
        if (clean.isBlank() || !clean.matches(".*\\d.*")) {
            return ProviderResult.failure(getName(), getMode(), "evaluate",
                    ProviderErrorCode.INVALID_REQUEST, "Expression is not numeric", elapsed(startMs));
        }
        try {
            double value;
            if (clean.contains("*")) {
                String[] parts = clean.split("\\*");
                double prod = 1.0;
                boolean any = false;
                for (String part : parts) {
                    if (!part.trim().isEmpty()) {
                        prod *= Double.parseDouble(part.trim());
                        any = true;
                    }
                }
                if (!any) {
                    return ProviderResult.failure(getName(), getMode(), "evaluate",
                            ProviderErrorCode.INVALID_REQUEST, "Expression is not numeric", elapsed(startMs));
                }
                value = prod;
            } else {
                String[] tokens = clean.split("\\+");
                double sum = 0.0;
                boolean any = false;
                for (String token : tokens) {
                    String trimmed = token.trim();
                    if (!trimmed.isEmpty()) {
                        sum += Double.parseDouble(trimmed);
                        any = true;
                    }
                }
                if (!any) {
                    return ProviderResult.failure(getName(), getMode(), "evaluate",
                            ProviderErrorCode.INVALID_REQUEST, "Expression is not numeric", elapsed(startMs));
                }
                value = sum;
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("expression", expression);
            data.put("result", value);
            String formatted = String.format(Locale.US, "%.2f", value);
            return ProviderResult.success(getName(), getMode(), "evaluate", data, formatted, elapsed(startMs));
        } catch (NumberFormatException ex) {
            return ProviderResult.failure(getName(), getMode(), "evaluate",
                    ProviderErrorCode.INVALID_REQUEST, "Expression is not numeric", elapsed(startMs));
        }
    }

    private static long elapsed(long startMs) {
        return Math.max(0, System.currentTimeMillis() - startMs);
    }
}
