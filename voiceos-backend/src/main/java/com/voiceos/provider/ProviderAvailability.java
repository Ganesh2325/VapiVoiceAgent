package com.voiceos.provider;

/**
 * Local configuration state. This is not an external health probe and does not
 * invent uptime percentages.
 */
public enum ProviderAvailability {
    AVAILABLE,
    DISABLED,
    MISCONFIGURED,
    UNAVAILABLE
}
