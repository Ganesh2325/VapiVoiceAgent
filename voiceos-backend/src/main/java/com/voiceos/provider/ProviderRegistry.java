package com.voiceos.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Server-side provider catalog. Clients cannot pick an implementation by class name.
 */
@Service
public class ProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(ProviderRegistry.class);

    private final Map<String, ServiceProvider> byName = new LinkedHashMap<>();
    private final Map<ProviderCapability, ServiceProvider> byCapability = new LinkedHashMap<>();

    public ProviderRegistry(List<ServiceProvider> providers) {
        if (providers == null) {
            return;
        }
        for (ServiceProvider provider : providers) {
            register(provider);
        }
        log.info("ProviderRegistry initialized with {} provider(s): {}", byName.size(), byName.keySet());
    }

    public void register(ServiceProvider provider) {
        if (provider == null || provider.getName() == null) {
            return;
        }
        ServiceProvider previous = byName.put(provider.getName(), provider);
        if (previous != null && previous != provider) {
            throw new IllegalStateException("Duplicate provider name: " + provider.getName());
        }
        Set<ProviderCapability> capabilities = provider.capabilities();
        if (capabilities == null) {
            return;
        }
        for (ProviderCapability capability : capabilities) {
            ServiceProvider owner = byCapability.putIfAbsent(capability, provider);
            if (owner != null && owner != provider) {
                log.warn("Capability {} already owned by {}; ignoring {}", capability, owner.getName(), provider.getName());
            }
        }
    }

    public Optional<ServiceProvider> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byName.get(name));
    }

    public Optional<ServiceProvider> findByCapability(ProviderCapability capability) {
        if (capability == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byCapability.get(capability));
    }

    public List<ServiceProvider> all() {
        return List.copyOf(byName.values());
    }
}
