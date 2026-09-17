package com.voiceos.provider.travel;

import com.voiceos.provider.ProviderRequest;
import com.voiceos.provider.ProviderResult;
import com.voiceos.provider.ServiceProvider;

/**
 * Travel operations. Phase 4 implementations are MOCK only.
 */
public interface TravelProvider extends ServiceProvider {

    ProviderResult search(ProviderRequest request);

    ProviderResult book(ProviderRequest request);
}
