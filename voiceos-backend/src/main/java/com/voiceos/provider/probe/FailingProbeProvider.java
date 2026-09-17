package com.voiceos.provider.probe;

import com.voiceos.provider.ProviderAvailability;
import com.voiceos.provider.ProviderCapability;
import com.voiceos.provider.ProviderErrorCode;
import com.voiceos.provider.ProviderMode;
import com.voiceos.provider.ProviderRequest;
import com.voiceos.provider.ProviderResult;
import com.voiceos.provider.ServiceProvider;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Test-only MOCK provider that always fails. Never reports success.
 */
@Component
public class FailingProbeProvider implements ServiceProvider {

    @Override
    public String getName() {
        return "FailingProbeProvider";
    }

    @Override
    public String getDescription() {
        return "[MOCK] Controlled provider failure for pipeline tests.";
    }

    @Override
    public ProviderMode getMode() {
        return ProviderMode.MOCK;
    }

    @Override
    public Set<ProviderCapability> capabilities() {
        return Set.of(ProviderCapability.PROBE_FAIL);
    }

    @Override
    public ProviderAvailability availability() {
        return ProviderAvailability.AVAILABLE;
    }

    @Override
    public boolean supportsOperation(String operation) {
        return operation != null && operation.toLowerCase().contains("fail");
    }

    @Override
    public ProviderResult execute(ProviderRequest request) {
        String operation = request != null ? request.operation() : "fail";
        if (request != null && "timeout".equalsIgnoreCase(request.paramAsString("simulate"))) {
            return ProviderResult.timeout(getName(), getMode(), operation, "MOCK: simulated provider timeout", 1);
        }
        return ProviderResult.failure(getName(), getMode(), operation,
                ProviderErrorCode.SIMULATED_FAILURE, "Controlled provider failure", 1);
    }
}
