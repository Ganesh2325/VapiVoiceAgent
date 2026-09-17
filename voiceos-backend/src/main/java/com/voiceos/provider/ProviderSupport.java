package com.voiceos.provider;

/**
 * Shared availability and no-silent-fallback checks.
 */
public final class ProviderSupport {

    public static final String NO_MOCK_FALLBACK =
            "REAL provider is not configured; refusing silent MOCK fallback";

    private ProviderSupport() {}

    public static ProviderAvailability availability(ProviderBinding binding, boolean realImplementationReady) {
        if (binding == null || !binding.enabledOrDefault()) {
            return ProviderAvailability.DISABLED;
        }
        if (binding.modeOr(ProviderMode.MOCK) == ProviderMode.REAL && !realImplementationReady) {
            return ProviderAvailability.MISCONFIGURED;
        }
        return ProviderAvailability.AVAILABLE;
    }

    public static ProviderResult rejectIfUnavailable(ServiceProvider provider, ProviderRequest request, long startMs) {
        ProviderAvailability availability = provider.availability();
        if (availability == ProviderAvailability.AVAILABLE) {
            return null;
        }
        long duration = Math.max(0, System.currentTimeMillis() - startMs);
        ProviderErrorCode code = availability == ProviderAvailability.MISCONFIGURED
                ? ProviderErrorCode.CONFIGURATION_ERROR
                : ProviderErrorCode.UNAVAILABLE;
        String message = availability == ProviderAvailability.MISCONFIGURED
                ? NO_MOCK_FALLBACK
                : "Provider is " + availability.name();
        return ProviderResult.unavailable(provider.getName(), provider.getMode(), request.operation(), code, message, duration);
    }
}
