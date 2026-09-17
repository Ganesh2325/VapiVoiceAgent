package com.voiceos.provider.payment;

import com.voiceos.action.audit.SensitiveDataRedactor;
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

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Honest MOCK payment adapter. Quote may be simulated. Capture/authorize/refund are unsupported.
 * Card numbers, OTPs, and payment credentials are never stored.
 */
@Service
public class MockPaymentProvider implements PaymentProvider {

    private final ProviderBinding binding;

    public MockPaymentProvider() {
        this(ProviderBinding.mockDefault());
    }

    public MockPaymentProvider(ProviderBinding binding) {
        this.binding = binding != null ? binding : ProviderBinding.mockDefault();
    }

    @Autowired
    public MockPaymentProvider(@Autowired(required = false) VoiceOsProperties properties) {
        this(properties != null && properties.providers() != null
                ? properties.providers().payment()
                : ProviderBinding.mockDefault());
    }

    @Override
    public String getName() {
        return "MockPaymentProvider";
    }

    @Override
    public String getDescription() {
        return "[MOCK] Simulated payment quotes only. Does not process cards or capture funds.";
    }

    @Override
    public ProviderMode getMode() {
        return binding.modeOr(ProviderMode.MOCK);
    }

    @Override
    public Set<ProviderCapability> capabilities() {
        return Set.of(ProviderCapability.PAYMENT);
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
        return normalized.contains("pay") || normalized.contains("quote")
                || normalized.contains("capture") || normalized.contains("refund")
                || normalized.contains("authorize");
    }

    @Override
    public ProviderResult execute(ProviderRequest request) {
        String operation = request != null ? request.operation().toLowerCase(Locale.ROOT) : "quote";
        if (operation.contains("capture")) {
            return capture(request);
        }
        if (operation.contains("refund")) {
            return refund(request);
        }
        if (operation.contains("auth")) {
            return authorize(request);
        }
        return quote(request);
    }

    @Override
    public ProviderResult quote(ProviderRequest request) {
        long startMs = System.currentTimeMillis();
        ProviderRequest safe = redact(request);
        ProviderResult blocked = ProviderSupport.rejectIfUnavailable(this, safe, startMs);
        if (blocked != null) {
            return blocked;
        }
        if (containsPaymentSecret(request)) {
            return ProviderResult.failure(getName(), ProviderMode.MOCK, "quote",
                    ProviderErrorCode.INVALID_REQUEST, "Payment credentials must not be submitted", elapsed(startMs));
        }
        Map<String, Object> data = ProviderResults.simulatedData(Map.of("amount", "unavailable"));
        return ProviderResult.success(getName(), ProviderMode.MOCK, "quote", data,
                "MOCK: simulated payment quote — no charge was made", elapsed(startMs));
    }

    @Override
    public ProviderResult authorize(ProviderRequest request) {
        return unsupported("authorize");
    }

    @Override
    public ProviderResult capture(ProviderRequest request) {
        return unsupported("capture");
    }

    @Override
    public ProviderResult refund(ProviderRequest request) {
        return unsupported("refund");
    }

    private ProviderResult unsupported(String operation) {
        return ProviderResult.failure(getName(), getMode(), operation,
                ProviderErrorCode.UNSUPPORTED_OPERATION,
                "MOCK: payment " + operation + " is not implemented and was not processed", 0);
    }

    private static ProviderRequest redact(ProviderRequest request) {
        if (request == null) {
            return new ProviderRequest("quote", Map.of(), null);
        }
        return new ProviderRequest(request.operation(), SensitiveDataRedactor.redact(request.params()), request.timeoutMs());
    }

    private static boolean containsPaymentSecret(ProviderRequest request) {
        if (request == null || request.params() == null) {
            return false;
        }
        for (String key : request.params().keySet()) {
            String lower = key.toLowerCase(Locale.ROOT);
            if (lower.contains("card") || lower.contains("otp") || lower.contains("cvv")
                    || lower.contains("cvc") || lower.contains("pan")) {
                return true;
            }
        }
        return false;
    }

    private static long elapsed(long startMs) {
        return Math.max(0, System.currentTimeMillis() - startMs);
    }
}
