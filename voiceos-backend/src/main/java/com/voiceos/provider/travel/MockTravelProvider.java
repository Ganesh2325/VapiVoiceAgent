package com.voiceos.provider.travel;

import com.voiceos.config.VoiceOsProperties;
import com.voiceos.provider.ProviderAvailability;
import com.voiceos.provider.ProviderBinding;
import com.voiceos.provider.ProviderCapability;
import com.voiceos.provider.ProviderErrorCode;
import com.voiceos.provider.ProviderMode;
import com.voiceos.provider.ProviderRequest;
import com.voiceos.provider.ProviderResult;
import com.voiceos.provider.ProviderResults;
import com.voiceos.provider.ProviderSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Honest MOCK travel adapter. Never implies a real airline or hotel booking.
 */
@Service
public class MockTravelProvider implements TravelProvider {

    private final ProviderBinding binding;

    public MockTravelProvider() {
        this(ProviderBinding.mockDefault());
    }

    public MockTravelProvider(ProviderBinding binding) {
        this.binding = binding != null ? binding : ProviderBinding.mockDefault();
    }

    @Autowired
    public MockTravelProvider(@Autowired(required = false) VoiceOsProperties properties) {
        this(properties != null && properties.providers() != null
                ? properties.providers().travel()
                : ProviderBinding.mockDefault());
    }

    @Override
    public String getName() {
        return "MockTravelProvider";
    }

    @Override
    public String getDescription() {
        return "[MOCK] Simulated travel search. Does not call a real airline or hotel system.";
    }

    @Override
    public ProviderMode getMode() {
        return binding.modeOr(ProviderMode.MOCK);
    }

    @Override
    public Set<ProviderCapability> capabilities() {
        return Set.of(ProviderCapability.TRAVEL_SEARCH, ProviderCapability.TRAVEL_BOOK);
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
        return normalized.contains("travel") || normalized.contains("flight")
                || normalized.contains("hotel") || normalized.contains("search")
                || normalized.contains("book");
    }

    @Override
    public ProviderResult execute(ProviderRequest request) {
        long startMs = System.currentTimeMillis();
        ProviderResult blocked = ProviderSupport.rejectIfUnavailable(this, request, startMs);
        if (blocked != null) {
            return blocked;
        }
        String operation = request.operation().toLowerCase(Locale.ROOT);
        if (operation.contains("book")) {
            return book(request);
        }
        return search(request);
    }

    @Override
    public ProviderResult search(ProviderRequest request) {
        long startMs = System.currentTimeMillis();
        ProviderResult blocked = ProviderSupport.rejectIfUnavailable(this, request != null ? request : new ProviderRequest("search", Map.of(), null), startMs);
        if (blocked != null) {
            return blocked;
        }
        Map<String, Object> data = ProviderResults.simulatedData(Map.of(
                "flights", List.of(
                        Map.of("id", "SIM-1", "route", "HYD-BLR", "label", "MOCK simulated itinerary")
                )
        ));
        return ProviderResult.success(getName(), ProviderMode.MOCK, "search", data,
                "MOCK: simulated flight search result", elapsed(startMs));
    }

    @Override
    public ProviderResult book(ProviderRequest request) {
        long startMs = System.currentTimeMillis();
        ProviderResult blocked = ProviderSupport.rejectIfUnavailable(this, request != null ? request : new ProviderRequest("book", Map.of(), null), startMs);
        if (blocked != null) {
            return blocked;
        }
        Map<String, Object> data = ProviderResults.simulatedData(Map.of(
                "bookingId", "SIMULATED-NOT-A-TICKET"
        ));
        return ProviderResult.success(getName(), ProviderMode.MOCK, "book", data,
                "MOCK: simulated booking request — not a real ticket", elapsed(startMs));
    }

    private static long elapsed(long startMs) {
        return Math.max(0, System.currentTimeMillis() - startMs);
    }
}
