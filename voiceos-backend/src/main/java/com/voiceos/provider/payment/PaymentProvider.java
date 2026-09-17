package com.voiceos.provider.payment;

import com.voiceos.provider.ProviderRequest;
import com.voiceos.provider.ProviderResult;
import com.voiceos.provider.ServiceProvider;

/**
 * Payment operations for a later phase. Phase 4 must not capture or store cards.
 */
public interface PaymentProvider extends ServiceProvider {

    ProviderResult quote(ProviderRequest request);

    ProviderResult authorize(ProviderRequest request);

    ProviderResult capture(ProviderRequest request);

    ProviderResult refund(ProviderRequest request);
}
