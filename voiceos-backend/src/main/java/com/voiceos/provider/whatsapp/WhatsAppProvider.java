package com.voiceos.provider.whatsapp;

import com.voiceos.provider.ProviderRequest;
import com.voiceos.provider.ProviderResult;
import com.voiceos.provider.ServiceProvider;

/**
 * WhatsApp operations. Phase 4 implementations are MOCK only.
 */
public interface WhatsAppProvider extends ServiceProvider {

    ProviderResult send(ProviderRequest request);
}
