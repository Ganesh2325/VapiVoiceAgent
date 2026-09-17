package com.voiceos.provider;

import com.voiceos.provider.calculator.RealLocalCalculatorProvider;
import com.voiceos.provider.calendar.MockCalendarProvider;
import com.voiceos.provider.email.MockEmailProvider;
import com.voiceos.provider.payment.MockPaymentProvider;
import com.voiceos.provider.probe.FailingProbeProvider;
import com.voiceos.provider.travel.MockTravelProvider;
import com.voiceos.provider.whatsapp.MockWhatsAppProvider;
import com.voiceos.tool.core.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderHonestyTest {

    @Test
    void realCalculatorComputesInputAndDoesNotHardcode() {
        RealLocalCalculatorProvider provider = new RealLocalCalculatorProvider();
        ProviderResult result = provider.evaluate("125 * 24");
        assertEquals(ProviderOutcome.SUCCESS, result.outcome());
        assertEquals(ProviderMode.REAL, result.mode());
        assertEquals("RealLocalCalculatorProvider", result.providerName());
        assertEquals("3000.00", result.message());
        assertEquals(3000.0, ((Number) result.data().get("result")).doubleValue());
        assertNotEquals("fixture", result.message());
    }

    @Test
    void invalidCalculatorExpressionIsFailureNotZero() {
        ProviderResult result = new RealLocalCalculatorProvider().evaluate("not-a-number");
        assertEquals(ProviderOutcome.FAILURE, result.outcome());
        assertEquals(ProviderErrorCode.INVALID_REQUEST, result.errorCode());
        assertNotEquals("0.00", result.message());
        ToolResult tool = ProviderResults.toToolResult(result);
        assertFalse(tool.success());
    }

    @Test
    void calculatorMockModeDoesNotSilentlyFallBack() {
        RealLocalCalculatorProvider provider = new RealLocalCalculatorProvider(
                new com.voiceos.config.VoiceOsProperties(
                        null, null, null, null, null, null, null, null,
                        new com.voiceos.config.VoiceOsProperties.ProvidersProperties(
                                new ProviderBinding(true, ProviderMode.MOCK, 5000L),
                                null, null, null, null, null
                        )
                )
        );
        assertEquals(ProviderAvailability.MISCONFIGURED, provider.availability());
        ProviderResult result = provider.execute(new ProviderRequest("evaluate", Map.of("expression", "1+1"), 1000L));
        assertEquals(ProviderOutcome.UNAVAILABLE, result.outcome());
        assertEquals(ProviderErrorCode.CONFIGURATION_ERROR, result.errorCode());
        assertFalse(result.success());
        assertNotEquals("2.00", result.message());
    }

    @Test
    void mockTravelIsSimulatedAndNeverBooks() {
        MockTravelProvider provider = new MockTravelProvider();
        assertEquals(ProviderMode.MOCK, provider.getMode());
        ProviderResult search = provider.search(new ProviderRequest("search", Map.of(), 1000L));
        assertEquals(ProviderMode.MOCK, search.mode());
        assertEquals("SIMULATED", search.data().get("status"));
        assertTrue(search.message().startsWith("MOCK:"));
        assertFalse(search.message().toLowerCase().contains("booking confirmed"));
        ProviderResult book = provider.book(new ProviderRequest("book", Map.of(), 1000L));
        assertTrue(book.message().contains("not a real ticket"));
        assertFalse(book.message().toLowerCase().contains("booking confirmed"));
    }

    @Test
    void realTravelWithoutImplementationIsUnavailable() {
        MockTravelProvider provider = new MockTravelProvider(new ProviderBinding(true, ProviderMode.REAL, 1000L));
        assertEquals(ProviderAvailability.MISCONFIGURED, provider.availability());
        ProviderResult result = provider.search(new ProviderRequest("search", Map.of(), 1000L));
        assertEquals(ProviderOutcome.UNAVAILABLE, result.outcome());
        assertEquals(ProviderErrorCode.CONFIGURATION_ERROR, result.errorCode());
        assertFalse(result.message().toLowerCase().contains("found 3 flights"));
    }

    @Test
    void mockWhatsAppDoesNotClaimDelivery() {
        MockWhatsAppProvider provider = new MockWhatsAppProvider();
        ProviderResult result = provider.send(new ProviderRequest("send", Map.of("message", "hi"), 1000L));
        assertEquals(ProviderMode.MOCK, result.mode());
        assertEquals("MOCK: WhatsApp message simulated", result.message());
        assertEquals(Boolean.FALSE, result.data().get("delivered"));
        assertFalse(result.message().toLowerCase().contains("delivered"));
    }

    @Test
    void mockEmailDoesNotClaimSent() {
        MockEmailProvider provider = new MockEmailProvider();
        ProviderResult result = provider.send(new ProviderRequest("send", Map.of(
                "recipient", "user@example.com", "subject", "s", "body", "b"
        ), 1000L));
        assertEquals(ProviderMode.MOCK, result.mode());
        assertTrue(result.message().startsWith("MOCK:"));
        assertEquals(Boolean.FALSE, result.data().get("sent"));
        assertFalse(result.message().toLowerCase().contains("sent successfully"));
    }

    @Test
    void mockCalendarReadIsSimulated() {
        ProviderResult result = new MockCalendarProvider().read(new ProviderRequest("read", Map.of(), 1000L));
        assertEquals(ProviderMode.MOCK, result.mode());
        assertEquals("SIMULATED", result.data().get("status"));
        assertTrue(result.message().startsWith("MOCK:"));
    }

    @Test
    void mockPaymentRejectsCardsAndDoesNotCapture() {
        MockPaymentProvider provider = new MockPaymentProvider();
        ProviderResult quote = provider.quote(new ProviderRequest("quote", Map.of("amount", "10"), 1000L));
        assertEquals(ProviderMode.MOCK, quote.mode());
        assertTrue(quote.message().contains("no charge was made"));
        ProviderResult capture = provider.capture(new ProviderRequest("capture", Map.of(), 1000L));
        assertEquals(ProviderErrorCode.UNSUPPORTED_OPERATION, capture.errorCode());
        assertFalse(capture.success());
        ProviderResult withCard = provider.quote(new ProviderRequest("quote", Map.of("cardNumber", "4111111111111111"), 1000L));
        assertEquals(ProviderErrorCode.INVALID_REQUEST, withCard.errorCode());
        assertFalse(String.valueOf(withCard.data()).contains("4111111111111111"));
        assertFalse(String.valueOf(withCard.message()).contains("4111111111111111"));
    }

    @Test
    void failingProbeIsMockFailureAndTimeoutMapsToToolFailure() {
        FailingProbeProvider provider = new FailingProbeProvider();
        assertEquals(ProviderMode.MOCK, provider.getMode());
        ProviderResult fail = provider.execute(new ProviderRequest("fail", Map.of(), 1000L));
        assertEquals(ProviderOutcome.FAILURE, fail.outcome());
        assertFalse(ProviderResults.toToolResult(fail).success());
        ProviderResult timeout = provider.execute(new ProviderRequest("fail", Map.of("simulate", "timeout"), 1000L));
        assertEquals(ProviderOutcome.TIMEOUT, timeout.outcome());
        ToolResult tool = ProviderResults.toToolResult(timeout);
        assertFalse(tool.success());
        assertTrue(tool.errorMessage().contains("TIMEOUT"));
    }

    @Test
    void providerRegistryLooksUpByCapabilityAndRejectsClientNameOverride() {
        ProviderRegistry registry = new ProviderRegistry(List.of(
                new RealLocalCalculatorProvider(),
                new MockTravelProvider()
        ));
        assertEquals("RealLocalCalculatorProvider",
                registry.findByCapability(ProviderCapability.CALCULATOR).orElseThrow().getName());
        assertTrue(registry.findByName("com.voiceos.provider.travel.MockTravelProvider").isEmpty());
        assertEquals("MockTravelProvider", registry.findByName("MockTravelProvider").orElseThrow().getName());
    }

    @Test
    void secretsAreRedactedFromProviderToolResult() {
        ProviderResult result = ProviderResult.success(
                "RealLocalCalculatorProvider",
                ProviderMode.REAL,
                "evaluate",
                Map.of("expression", "1+1", "jwt", "aaa.bbb.ccc", "password", "hunter2"),
                "2.00",
                1
        );
        ToolResult tool = ProviderResults.toToolResult(result);
        assertEquals("[REDACTED]", tool.data().get("jwt"));
        assertEquals("[REDACTED]", tool.data().get("password"));
        assertFalse(String.valueOf(tool.data()).contains("hunter2"));
    }

    @Test
    void structuredErrorCodesArePreserved() {
        ProviderResult result = ProviderResult.unavailable(
                "MockTravelProvider", ProviderMode.REAL, "search",
                ProviderErrorCode.CONFIGURATION_ERROR, ProviderSupport.NO_MOCK_FALLBACK, 0
        );
        ToolResult tool = ProviderResults.toToolResult(result);
        assertFalse(tool.success());
        assertTrue(tool.errorMessage().startsWith("CONFIGURATION_ERROR"));
        assertEquals("CONFIGURATION_ERROR", tool.data().get("errorCode"));
    }
}
