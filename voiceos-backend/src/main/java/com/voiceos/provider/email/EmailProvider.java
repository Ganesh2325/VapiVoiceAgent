package com.voiceos.provider.email;

import com.voiceos.provider.ProviderRequest;
import com.voiceos.provider.ProviderResult;
import com.voiceos.provider.ServiceProvider;

public interface EmailProvider extends ServiceProvider {

    ProviderResult send(ProviderRequest request);
}
