package com.voiceos.provider;

/**
 * Terminal result of a provider call. Failures are not encoded as success strings.
 */
public enum ProviderOutcome {
    SUCCESS,
    FAILURE,
    UNAVAILABLE,
    TIMEOUT
}
