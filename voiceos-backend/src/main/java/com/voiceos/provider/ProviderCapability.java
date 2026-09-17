package com.voiceos.provider;

/**
 * Logical operations a provider may own. Selection is server-side by capability,
 * never by a client-supplied class name.
 */
public enum ProviderCapability {
    CALCULATOR,
    TRAVEL_SEARCH,
    TRAVEL_BOOK,
    EMAIL_SEND,
    WHATSAPP_SEND,
    CALENDAR_READ,
    CALENDAR_WRITE,
    PAYMENT,
    PROBE_FAIL
}
