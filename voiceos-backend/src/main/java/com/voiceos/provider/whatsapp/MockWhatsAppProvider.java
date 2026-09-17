package com.voiceos.provider.whatsapp;

import com.voiceos.config.VoiceOsProperties;
import com.voiceos.provider.ProviderAvailability;
import com.voiceos.provider.ProviderBinding;
import com.voiceos.provider.ProviderCapability;
import com.voiceos.provider.ProviderMode;
import com.voiceos.provider.ProviderRequest;
import com.voiceos.provider.ProviderResult;
import com.voiceos.provider.ProviderResults;
import com.voiceos.provider.ProviderSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Honest MOCK WhatsApp adapter. Never claims a real message was delivered.
 */
@Service
public class MockWhatsAppProvider implements WhatsAppProvider {

    private final ProviderBinding binding;

    public MockWhatsAppProvider() {
        this(ProviderBinding.mockDefault());
    }

    public MockWhatsAppProvider(ProviderBinding binding) {
        this.binding = binding != null ? binding : ProviderBinding.mockDefault();
    }

    @Autowired
    public MockWhatsAppProvider(@Autowired(required = false) VoiceOsProperties properties) {
        this(properties != null && properties.providers() != null
                ? properties.providers().whatsapp()
                : ProviderBinding.mockDefault());
    }

    @Override
    public String getName() {
        return "MockWhatsAppProvider";
    }

    @Override
    public String getDescription() {
        return "[MOCK] Simulated WhatsApp send. Does not deliver a real WhatsApp message.";
    }

    @Override
    public ProviderMode getMode() {
        return binding.modeOr(ProviderMode.MOCK);
    }

    @Override
    public Set<ProviderCapability> capabilities() {
        return Set.of(ProviderCapability.WHATSAPP_SEND);
    }

    @Override
    public ProviderAvailability availability() {
        return ProviderSupport.availability(binding, false);
    }

    @Override
    public boolean supportsOperation(String operation) {
        if (operation == null) {
            return false;
        }
        String normalized = operation.toLowerCase(Locale.ROOT);
        return normalized.contains("whatsapp") || normalized.contains("message") || normalized.contains("send");
    }

    @Override
    public ProviderResult execute(ProviderRequest request) {
        return send(request);
    }

    @Override
    public ProviderResult send(ProviderRequest request) {
        long startMs = System.currentTimeMillis();
        ProviderRequest safe = request != null ? request : new ProviderRequest("send", Map.of(), null);
        ProviderResult blocked = ProviderSupport.rejectIfUnavailable(this, safe, startMs);
        if (blocked != null) {
            return blocked;
        }
        Map<String, Object> data = ProviderResults.simulatedData(Map.of(
                "delivered", false
        ));
        return ProviderResult.success(getName(), ProviderMode.MOCK, "send", data,
                "MOCK: WhatsApp message simulated", elapsed(startMs));
    }

    private static long elapsed(long startMs) {
        return Math.max(0, System.currentTimeMillis() - startMs);
    }
}
