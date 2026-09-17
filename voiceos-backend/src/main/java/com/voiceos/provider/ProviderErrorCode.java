package com.voiceos.provider;

/**
 * Structured provider errors. Safe codes may be persisted; stack traces and
 * credentials must not.
 */
public enum ProviderErrorCode {
    CONFIGURATION_ERROR,
    AUTHENTICATION_ERROR,
    AUTHORIZATION_ERROR,
    RATE_LIMITED,
    NETWORK_ERROR,
    TIMEOUT,
    REMOTE_ERROR,
    INVALID_REQUEST,
    UNAVAILABLE,
    UNSUPPORTED_OPERATION,
    SIMULATED_FAILURE
}
