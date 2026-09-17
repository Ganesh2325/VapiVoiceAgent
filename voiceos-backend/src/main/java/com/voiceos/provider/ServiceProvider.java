package com.voiceos.provider;

import java.util.Set;

/**
 * Base contract for VoiceOS business providers (calculator, travel, messaging, …).
 * Implementations must declare {@link ProviderMode} explicitly and never claim
 * REAL success for a simulated operation.
 */
public interface ServiceProvider {

    String getName();

    String getDescription();

    ProviderMode getMode();

    Set<ProviderCapability> capabilities();

    ProviderAvailability availability();

    boolean supportsOperation(String operation);

    ProviderResult execute(ProviderRequest request);

    default boolean supportsIntent(String intent) {
        return supportsOperation(intent);
    }
}
