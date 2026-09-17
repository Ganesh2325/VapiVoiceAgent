package com.voiceos.provider.calendar;

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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Honest MOCK calendar adapter. Events are fixtures, not a real calendar.
 */
@Service
public class MockCalendarProvider implements CalendarProvider {

    private final ProviderBinding binding;

    public MockCalendarProvider() {
        this(ProviderBinding.mockDefault());
    }

    public MockCalendarProvider(ProviderBinding binding) {
        this.binding = binding != null ? binding : ProviderBinding.mockDefault();
    }

    @Autowired
    public MockCalendarProvider(@Autowired(required = false) VoiceOsProperties properties) {
        this(properties != null && properties.providers() != null
                ? properties.providers().calendar()
                : ProviderBinding.mockDefault());
    }

    @Override
    public String getName() {
        return "MockCalendarProvider";
    }

    @Override
    public String getDescription() {
        return "[MOCK] Simulated calendar. Does not read or write a real calendar.";
    }

    @Override
    public ProviderMode getMode() {
        return binding.modeOr(ProviderMode.MOCK);
    }

    @Override
    public Set<ProviderCapability> capabilities() {
        return Set.of(ProviderCapability.CALENDAR_READ, ProviderCapability.CALENDAR_WRITE);
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
        return normalized.contains("calendar") || normalized.contains("schedule")
                || normalized.contains("event") || normalized.contains("create_event");
    }

    @Override
    public ProviderResult execute(ProviderRequest request) {
        String operation = request != null ? request.operation() : "read";
        if (operation.toLowerCase(Locale.ROOT).contains("create") || operation.toLowerCase(Locale.ROOT).contains("write")) {
            return write(request);
        }
        return read(request);
    }

    @Override
    public ProviderResult read(ProviderRequest request) {
        long startMs = System.currentTimeMillis();
        ProviderRequest safe = request != null ? request : new ProviderRequest("read", Map.of(), null);
        ProviderResult blocked = ProviderSupport.rejectIfUnavailable(this, safe, startMs);
        if (blocked != null) {
            return blocked;
        }
        Map<String, Object> data = ProviderResults.simulatedData(Map.of(
                "events", List.of(Map.of("title", "MOCK simulated event", "source", "fixture"))
        ));
        return ProviderResult.success(getName(), ProviderMode.MOCK, "read", data,
                "MOCK: simulated calendar read", elapsed(startMs));
    }

    @Override
    public ProviderResult write(ProviderRequest request) {
        long startMs = System.currentTimeMillis();
        ProviderRequest safe = request != null ? request : new ProviderRequest("write", Map.of(), null);
        ProviderResult blocked = ProviderSupport.rejectIfUnavailable(this, safe, startMs);
        if (blocked != null) {
            return blocked;
        }
        Map<String, Object> data = ProviderResults.simulatedData(Map.of(
                "written", false
        ));
        return ProviderResult.success(getName(), ProviderMode.MOCK, "write", data,
                "MOCK: simulated calendar write — not persisted to a real calendar", elapsed(startMs));
    }

    private static long elapsed(long startMs) {
        return Math.max(0, System.currentTimeMillis() - startMs);
    }
}
