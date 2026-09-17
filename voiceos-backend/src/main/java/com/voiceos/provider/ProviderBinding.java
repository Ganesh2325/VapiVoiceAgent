package com.voiceos.provider;

/**
 * Per-provider configuration. Credentials never belong here.
 */
public record ProviderBinding(
        Boolean enabled,
        ProviderMode mode,
        Long timeoutMs
) {
    public static ProviderBinding mockDefault() {
        return new ProviderBinding(true, ProviderMode.MOCK, 5_000L);
    }

    public static ProviderBinding realDefault() {
        return new ProviderBinding(true, ProviderMode.REAL, 5_000L);
    }

    public boolean enabledOrDefault() {
        return enabled == null || enabled;
    }

    public ProviderMode modeOr(ProviderMode fallback) {
        return mode != null ? mode : fallback;
    }

    public long timeoutOrDefault() {
        return timeoutMs != null && timeoutMs > 0 ? timeoutMs : 5_000L;
    }
}
