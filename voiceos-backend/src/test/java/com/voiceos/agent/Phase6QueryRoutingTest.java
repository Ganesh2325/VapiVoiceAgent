package com.voiceos.agent;

import com.voiceos.action.engine.ActionEngine;
import com.voiceos.action.engine.ResultVerifier;
import com.voiceos.action.model.ActionCommand;
import com.voiceos.action.model.ActionExecutionResult;
import com.voiceos.action.model.ActionStatus;
import com.voiceos.action.store.InMemoryActionStore;
import com.voiceos.action.store.InMemoryActionStepStore;
import com.voiceos.action.store.InMemoryAuditEventStore;
import com.voiceos.action.audit.ActionTraceService;
import com.voiceos.agent.core.AgentOutcome;
import com.voiceos.agent.core.AgentRegistry;
import com.voiceos.agent.core.AgentRequest;
import com.voiceos.agent.core.AgentResult;
import com.voiceos.agent.core.AgentSelector;
import com.voiceos.agent.core.RegistryAgentSelector;
import com.voiceos.agent.impl.EmailAgent;
import com.voiceos.agent.impl.GeneralQueryAgent;
import com.voiceos.agent.impl.TravelAgent;
import com.voiceos.agent.impl.UtilityAgent;
import com.voiceos.ai.LLMProvider;
import com.voiceos.ai.LLMRequest;
import com.voiceos.ai.LLMResponse;
import com.voiceos.ai.UnavailableLLMProvider;
import com.voiceos.ai.gemini.LLMProviderException;
import com.voiceos.ai.mock.MockLLMProvider;
import com.voiceos.service.PolicyEngine;
import com.voiceos.tool.core.ToolRegistry;
import com.voiceos.tool.impl.CalculatorTool;
import com.voiceos.tool.impl.SearchTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase6QueryRoutingTest {

    private static final UUID USER = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private AgentSelector selector;
    private ActionEngine actionEngine;
    private GeneralQueryAgent general;

    @BeforeEach
    void setUp() {
        ToolRegistry tools = new ToolRegistry(List.of(
                new CalculatorTool(new com.voiceos.provider.calculator.RealLocalCalculatorProvider()),
                new SearchTool()));
        general = new GeneralQueryAgent(new MockLLMProvider(), null);
        AgentRegistry registry = new AgentRegistry(List.of(
                new UtilityAgent(tools),
                new TravelAgent(tools),
                new EmailAgent(tools),
                general
        ));
        selector = new RegistryAgentSelector(registry);
        actionEngine = new ActionEngine(
                new InMemoryActionStore(),
                new PolicyEngine(),
                selector,
                tools,
                new ResultVerifier(),
                null, null, null, null, null,
                new ActionTraceService(new InMemoryActionStepStore(), new InMemoryAuditEventStore())
        );
    }

    @Test
    void generalQuestionRoutesToGeneralQueryAgent() {
        AgentRequest request = new AgentRequest(USER, null, "r", null, null,
                "What is the difference between REST and GraphQL?", null, Map.of(), null);
        assertEquals("GeneralQueryAgent", selector.select(request).orElseThrow().getName());
    }

    @Test
    void calculatorRoutesToUtilityAgentNotGeneral() {
        AgentRequest request = new AgentRequest(USER, null, "r", null, null,
                "Calculate 125 multiplied by 24", null, Map.of(), null);
        assertEquals("UtilityAgent", selector.select(request).orElseThrow().getName());
    }

    @Test
    void travelRoutesToTravelAgentNotGeneral() {
        AgentRequest request = new AgentRequest(USER, null, "r", null, null,
                "Book me a flight from Delhi to London", null, Map.of(), null);
        assertEquals("TravelAgent", selector.select(request).orElseThrow().getName());
    }

    @Test
    void emailDraftRoutesToGeneralQueryAgent() {
        AgentRequest request = new AgentRequest(USER, null, "r", null, null,
                "Write a professional email asking my professor for an extension.", null, Map.of(), null);
        assertEquals("GeneralQueryAgent", selector.select(request).orElseThrow().getName());
    }

    @Test
    void emailSendRoutesToEmailAgent() {
        AgentRequest request = new AgentRequest(USER, null, "r", null, null,
                "Send that email to my professor.", null, Map.of(), null);
        assertEquals("EmailAgent", selector.select(request).orElseThrow().getName());
    }

    @Test
    void mockLlmIsLabeledMockAndNotHardcodedProductionFaq() {
        AgentResult result = general.execute(new AgentRequest(USER, null, "r", null, null,
                "What is dependency injection in Spring Boot?", null, Map.of(), null));
        assertEquals(AgentOutcome.COMPLETED, result.outcome());
        assertTrue(result.success());
        assertEquals("MOCK", result.stateUpdates().get("providerMode"));
        assertEquals("mock", result.stateUpdates().get("llmProvider"));
        assertTrue(result.responseText().contains("[MOCK DATA]"));
        assertFalse(result.responseText().toLowerCase().contains("hardcoded faq"));
        assertTrue(result.toolExecutions().isEmpty());
        assertEquals("GENERAL_QUESTION", result.stateUpdates().get("intent"));
        assertFalse(result.stateUpdates().containsKey("confidence"));
        assertFalse(result.responseText().toLowerCase().contains("confidence"));
    }

    @Test
    void missingLlmReturnsUnavailable() {
        GeneralQueryAgent agent = new GeneralQueryAgent(new UnavailableLLMProvider("gemini"), null);
        AgentResult result = agent.execute(new AgentRequest(USER, null, "r", null, null,
                "Explain Java streams.", null, Map.of(), null));
        assertEquals(AgentOutcome.UNAVAILABLE, result.outcome());
        assertFalse(result.success());
        assertTrue(result.responseText().toLowerCase().contains("unavailable"));
        assertEquals("UNAVAILABLE", result.stateUpdates().get("providerMode"));
    }

    @Test
    void llmFailureReturnsFailureNotFakeAnswer() {
        GeneralQueryAgent agent = new GeneralQueryAgent(new FailingLlm(), null);
        AgentResult result = agent.execute(new AgentRequest(USER, null, "r", null, null,
                "Explain Java streams.", null, Map.of(), null));
        assertEquals(AgentOutcome.FAILED, result.outcome());
        assertFalse(result.success());
        assertTrue(result.responseText().toLowerCase().contains("unavailable"));
        assertFalse(result.responseText().toLowerCase().contains("couldn't understand"));
    }

    @Test
    void modelCannotExecuteArbitraryToolOrProvider() {
        AgentResult result = general.execute(new AgentRequest(USER, null, "r", null, null, "hi",
                "send_email",
                Map.of("provider", "EvilProvider", "agent", "FinanceAgent", "url", "https://evil.example"),
                null));
        assertEquals("UNSUPPORTED_REQUEST", result.errorCode());
        assertTrue(result.toolExecutions().isEmpty());
        assertEquals(AgentOutcome.FAILED, result.outcome());
    }

    @Test
    void clientCannotForceAgentName() {
        AgentRequest request = new AgentRequest(USER, null, "r", null, null,
                "Calculate 125 multiplied by 24", null,
                Map.of("agent", "GeneralQueryAgent", "agentName", "TravelAgent"), null);
        assertFalse(request.parameters().containsKey("agent"));
        assertEquals("UtilityAgent", selector.select(request).orElseThrow().getName());
    }

    @Test
    void ambiguousQuestionAsksClarification() {
        AgentResult result = general.execute(new AgentRequest(USER, null, "r", null, null,
                "What's the best one?", null, Map.of(), null));
        assertEquals(AgentOutcome.NEEDS_INFORMATION, result.outcome());
        assertTrue(result.missingFields().contains("referent"));
    }

    @Test
    void realtimeWeatherIsUnavailableNotFabricated() {
        ActionExecutionResult result = actionEngine.execute(new ActionCommand(
                USER, null, null, "req-weather", null, "weather-1", "TEST",
                "What is the weather in London right now?", null, Map.of()
        ));
        assertEquals("GeneralQueryAgent", result.agent());
        assertEquals(ActionStatus.FAILED.name(), result.status());
        assertFalse(result.success());
        assertTrue(result.error().toLowerCase().contains("real-time")
                || result.error().toLowerCase().contains("unavailable"));
        assertFalse(String.valueOf(result.result()).matches(".*\\d+\\s?°.*"));
        assertNotEquals(ActionStatus.COMPLETED.name(), result.status());
    }

    @Test
    void calculatorPipelineStillCompletesThreeThousand() {
        ActionExecutionResult result = actionEngine.execute(new ActionCommand(
                USER, null, null, "req-calc", null, "calc-6", "TEST",
                "Calculate 125 multiplied by 24", "calculator", Map.of("expression", "125 * 24")
        ));
        assertEquals("UtilityAgent", result.agent());
        assertEquals(ActionStatus.COMPLETED.name(), result.status());
        assertEquals("3000.00", result.result());
        assertEquals("REAL", result.providerMode());
    }

    @Test
    void generalQuestionCompletesThroughActionEngineWithMockLlm() {
        ActionExecutionResult result = actionEngine.execute(new ActionCommand(
                USER, null, null, "req-di", null, "di-1", "TEST",
                "What is dependency injection in Spring Boot?", null, Map.of("agent", "TravelAgent")
        ));
        assertEquals("GeneralQueryAgent", result.agent());
        assertEquals(ActionStatus.COMPLETED.name(), result.status());
        assertEquals("mock", result.provider());
        assertEquals("MOCK", result.providerMode());
        assertTrue(result.responseText().contains("[MOCK DATA]"));
        assertTrue(result.tool() == null || result.tool().isBlank());
        assertFalse(result.responseText().toLowerCase().contains("confidence"));
    }

    @Test
    void writingRequestInfersWritingIntentWithoutSendingEmail() {
        AgentResult result = general.execute(new AgentRequest(USER, null, "r", null, null,
                "Write a professional email asking my professor for an extension.", null, Map.of(), null));
        assertEquals(AgentOutcome.COMPLETED, result.outcome());
        assertEquals("WRITING", result.stateUpdates().get("intent"));
        assertTrue(result.toolExecutions().isEmpty());
        assertTrue(result.responseText().contains("[MOCK DATA]"));
        assertFalse(result.responseText().toLowerCase().contains("email sent successfully"));
    }

    @Test
    void malformedCalculatorArgumentsAreRejected() {
        ActionExecutionResult result = actionEngine.execute(new ActionCommand(
                USER, null, null, "req-bad-calc", null, "calc-bad", "TEST",
                "Calculate this", "calculator", Map.of()
        ));
        assertEquals("UtilityAgent", result.agent());
        assertNotEquals(ActionStatus.COMPLETED.name(), result.status());
        assertFalse(result.success());
    }

    @Test
    void emailSendNeedsInformationAndDoesNotClaimSent() {
        ActionExecutionResult result = actionEngine.execute(new ActionCommand(
                USER, null, null, "req-email", null, "email-1", "TEST",
                "Send that email to my professor.", null, Map.of()
        ));
        assertEquals("EmailAgent", result.agent());
        assertEquals(ActionStatus.REQUIRES_APPROVAL.name(), result.status());
        assertFalse(result.success());
        assertFalse(String.valueOf(result.responseText()).toLowerCase().contains("email sent"));
    }

    private static final class FailingLlm implements LLMProvider {
        @Override
        public LLMResponse chat(LLMRequest request) {
            throw new LLMProviderException("upstream timeout");
        }

        @Override
        public java.util.List<Float> embed(String text) {
            throw new LLMProviderException("upstream timeout");
        }

        @Override
        public String getProviderName() {
            return "groq";
        }

        @Override
        public boolean isAvailable() {
            return true;
        }
    }
}
