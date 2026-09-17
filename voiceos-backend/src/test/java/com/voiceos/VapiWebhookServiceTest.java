package com.voiceos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voiceos.action.engine.ActionEngine;
import com.voiceos.action.engine.ResultVerifier;
import com.voiceos.action.audit.ActionTraceService;
import com.voiceos.action.store.InMemoryActionStore;
import com.voiceos.action.store.InMemoryActionStepStore;
import com.voiceos.action.store.InMemoryAuditEventStore;
import com.voiceos.agent.core.AgentRegistry;
import com.voiceos.agent.core.RegistryAgentSelector;
import com.voiceos.agent.impl.UtilityAgent;
import com.voiceos.config.VoiceOsProperties;
import com.voiceos.domain.entity.User;
import com.voiceos.domain.entity.VoiceSession;
import com.voiceos.domain.entity.WebhookEvent;
import com.voiceos.domain.repository.VoiceSessionRepository;
import com.voiceos.domain.repository.WebhookEventRepository;
import com.voiceos.service.PolicyEngine;
import com.voiceos.service.VoiceSessionService;
import com.voiceos.tool.core.ToolRegistry;
import com.voiceos.tool.impl.CalculatorTool;
import com.voiceos.tool.impl.ForbiddenTool;
import com.voiceos.tool.impl.ProbeFailTool;
import com.voiceos.tool.impl.SearchTool;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiAssistantResponse;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiCall;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiFunction;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiMessage;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiToolCall;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiToolCallResponse;
import com.voiceos.vapi.dto.VapiWebhookDTOs.VapiWebhookPayload;
import com.voiceos.vapi.service.VapiEventProcessor;
import com.voiceos.vapi.service.VapiIdempotencyKeyBuilder;
import com.voiceos.vapi.service.VapiIdempotencyService;
import com.voiceos.vapi.service.VapiToolService;
import com.voiceos.vapi.service.VapiWebhookSecurityService;
import com.voiceos.vapi.service.VapiWebhookService;
import com.voiceos.voice.VoiceConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VapiWebhookServiceTest {

    private static final UUID BOUND_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private VapiWebhookService vapiWebhookService;
    private VapiWebhookSecurityService securityService;
    private ToolRegistry toolRegistry;
    private ConcurrentHashMap<String, WebhookEvent> eventStore;
    private InMemoryActionStore actionStore;

    @BeforeEach
    void setUp() {
        CalculatorTool calcTool = new CalculatorTool(new com.voiceos.provider.calculator.RealLocalCalculatorProvider());
        SearchTool searchTool = new SearchTool();
        toolRegistry = new ToolRegistry(List.of(calcTool, searchTool, new ForbiddenTool(), new ProbeFailTool(new com.voiceos.provider.probe.FailingProbeProvider())));
        eventStore = new ConcurrentHashMap<>();
        vapiWebhookService = buildWebhookService(toolRegistry);
    }

    @Test
    void testSecretValidation() {
        assertTrue(vapiWebhookService.validateSecret("secret123"));
        assertFalse(vapiWebhookService.validateSecret("wrong_secret"));
        assertFalse(vapiWebhookService.validateSecret(null));
    }

    @Test
    void testBlankConfiguredSecretIsRejected() {
        VapiWebhookSecurityService failClosed = new VapiWebhookSecurityService(properties("", true, false));
        assertFalse(failClosed.isValid("anything"));
        assertFalse(failClosed.isValid(null));
    }

    @Test
    void testToolCallExecution() {
        VapiToolCall toolCall = new VapiToolCall(
                "call-1",
                "function",
                new VapiFunction("calculator", Map.of("expression", "25 * 4", "operation", "sum"))
        );

        VapiMessage msg = message("tool-calls", List.of(toolCall));
        VapiWebhookPayload payload = new VapiWebhookPayload(msg, new VapiCall("call-abc", "org-1", "ast-1", null, "webCall", "in-progress", null, null), "2026-08-11T00:00:00Z");

        Object result = vapiWebhookService.processWebhook(payload, "{}");
        assertTrue(result instanceof VapiToolCallResponse);

        VapiToolCallResponse response = (VapiToolCallResponse) result;
        assertEquals(1, response.results().size());
        assertEquals("100.00", response.results().get(0).result());
    }

    @Test
    void testDuplicateToolCallDoesNotExecuteTwice() {
        CountingCalculator calc = new CountingCalculator();
        toolRegistry = new ToolRegistry(List.of(calc, new SearchTool(), new ForbiddenTool(), new ProbeFailTool(new com.voiceos.provider.probe.FailingProbeProvider())));
        eventStore = new ConcurrentHashMap<>();
        vapiWebhookService = buildWebhookService(toolRegistry);

        VapiToolCall toolCall = new VapiToolCall(
                "stable-tool-call-1",
                "function",
                new VapiFunction("calculator", Map.of("expression", "10 + 5"))
        );
        VapiWebhookPayload payload = new VapiWebhookPayload(
                message("tool-calls", List.of(toolCall)),
                new VapiCall("call-dup", "org-1", "ast-1", null, "webCall", "in-progress", null, null),
                "2026-08-11T00:00:00Z"
        );

        Object first = vapiWebhookService.processWebhook(payload, "{}");
        Object second = vapiWebhookService.processWebhook(payload, "{}");

        assertEquals(1, calc.executions);
        assertEquals("15.00", extractCalculatorResult(first));
        assertEquals("15.00", extractCalculatorResult(second));
        assertEquals(1, eventStore.size());
    }

    @Test
    void testAssistantRequestResponse() {
        VapiMessage msg = message("assistant-request", null);
        VapiWebhookPayload payload = new VapiWebhookPayload(
                msg, new VapiCall("call-assistant", null, null, null, null, null, null, null), "2026-09-17T00:00:00Z");
        Object result = vapiWebhookService.processWebhook(payload, "{}");

        assertTrue(result instanceof VapiAssistantResponse);
        VapiAssistantResponse res = (VapiAssistantResponse) result;
        assertNotNull(res.assistant().get("firstMessage"));
    }

    @Test
    void testEmptyPayloadAcknowledged() {
        Object result = vapiWebhookService.processWebhook(null, "{}");
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) result;
        assertEquals("ok", body.get("status"));
    }

    @Test
    void testUnknownEventAcknowledged() {
        VapiMessage msg = message("not-a-real-event", null);
        Object result = vapiWebhookService.processWebhook(
                new VapiWebhookPayload(msg, new VapiCall("call-unknown", null, null, null, null, null, null, null), "t1"),
                "{}");
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) result;
        assertEquals("acknowledged", body.get("status"));
    }

    @Test
    void testMissingFunctionNameReturnsToolError() {
        VapiToolCall toolCall = new VapiToolCall("call-missing", "function", new VapiFunction(null, Map.of()));
        VapiMessage msg = message("tool-calls", List.of(toolCall));
        Object result = vapiWebhookService.processWebhook(
                new VapiWebhookPayload(msg, new VapiCall("call-err", null, null, null, null, null, null, null), "t1"),
                "{}");
        assertTrue(result instanceof VapiToolCallResponse);
        VapiToolCallResponse response = (VapiToolCallResponse) result;
        assertEquals(1, response.results().size());
        assertNotNull(response.results().get(0).error());
    }

    @Test
    void unboundVoiceSessionRejectsToolCall() {
        VoiceSessionService unbound = new VoiceSessionService(mockEmptySessionRepository());
        VapiToolService toolService = new VapiToolService(buildActionEngine(toolRegistry), unbound);
        vapiWebhookService = wrap(toolService);

        VapiToolCall toolCall = new VapiToolCall(
                "call-unbound",
                "function",
                new VapiFunction("calculator", Map.of("expression", "2 + 2"))
        );
        Object result = vapiWebhookService.processWebhook(
                new VapiWebhookPayload(message("tool-calls", List.of(toolCall)),
                        new VapiCall("call-unbound", null, null, null, null, null, null, null), "t1"),
                "{}");
        VapiToolCallResponse response = (VapiToolCallResponse) result;
        assertNotNull(response.results().get(0).error());
        assertTrue(response.results().get(0).error().toLowerCase().contains("session")
                || response.results().get(0).error().toLowerCase().contains("user"));
    }

    private VapiWebhookService buildWebhookService(ToolRegistry registry) {
        ActionEngine actionEngine = buildActionEngine(registry);
        VapiToolService toolService = new VapiToolService(actionEngine, boundSessionService());
        return wrap(toolService);
    }

    private ActionEngine buildActionEngine(ToolRegistry registry) {
        actionStore = new InMemoryActionStore();
        UtilityAgent utilityAgent = new UtilityAgent(registry);
        AgentRegistry agentRegistry = new AgentRegistry(List.of(utilityAgent));
        return new ActionEngine(
                actionStore,
                new PolicyEngine(),
                new RegistryAgentSelector(agentRegistry),
                registry,
                new ResultVerifier(),
                null,
                null,
                null,
                null,
                null,
                new ActionTraceService(new InMemoryActionStepStore(), new InMemoryAuditEventStore())
        );
    }

    private VapiWebhookService wrap(VapiToolService toolService) {
        VoiceOsProperties props = properties("secret123", true, false);
        securityService = new VapiWebhookSecurityService(props);
        return new VapiWebhookService(
                toolService,
                new VapiEventProcessor(null, null, null, null),
                securityService,
                new VapiIdempotencyService(mockRepository(eventStore), new ObjectMapper()),
                new VapiIdempotencyKeyBuilder(),
                new ObjectMapper(),
                new VoiceConfiguration()
        );
    }

    private static VoiceSessionService boundSessionService() {
        User user = new User("voice-test@example.com", "hash", "Voice Test");
        user.setId(BOUND_USER_ID);
        VoiceSession session = new VoiceSession();
        session.setUser(user);
        session.setStatus(VoiceSession.Status.BOUND);
        session.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));

        VoiceSessionRepository repo = mock(VoiceSessionRepository.class);
        when(repo.findByCallId(any())).thenAnswer(inv -> {
            session.setCallId(inv.getArgument(0));
            return Optional.of(session);
        });
        return new VoiceSessionService(repo);
    }

    private static VoiceSessionRepository mockEmptySessionRepository() {
        VoiceSessionRepository repo = mock(VoiceSessionRepository.class);
        when(repo.findByCallId(any())).thenReturn(Optional.empty());
        return repo;
    }

    private static String extractCalculatorResult(Object result) {
        if (result instanceof VapiToolCallResponse response) {
            return response.results().get(0).result();
        }
        if (result instanceof Map<?, ?> map && map.get("results") instanceof List<?> results && !results.isEmpty()) {
            Object first = results.get(0);
            if (first instanceof Map<?, ?> row) {
                return String.valueOf(row.get("result"));
            }
        }
        fail("Unexpected webhook result type: " + (result == null ? "null" : result.getClass().getName()));
        return null;
    }

    private static VapiMessage message(String type, List<VapiToolCall> toolCalls) {
        return new VapiMessage(
                type, null, toolCalls, null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );
    }

    private static VoiceOsProperties properties(String secret, boolean requireSecret, boolean allowInsecureLocal) {
        return new VoiceOsProperties(
                null, null,
                new VoiceOsProperties.VoiceProperties("vapi",
                        new VoiceOsProperties.VapiProperties("test_api_key", "test_ast", secret, "pk_123", "https://api.vapi.ai")),
                null,
                new VoiceOsProperties.WebhooksProperties(secret, null, null, requireSecret, allowInsecureLocal),
                null, null, null, null
        );
    }

    private static WebhookEventRepository mockRepository(ConcurrentHashMap<String, WebhookEvent> store) {
        WebhookEventRepository repo = mock(WebhookEventRepository.class);
        when(repo.findByIdempotencyKey(any())).thenAnswer(inv -> Optional.ofNullable(store.get(inv.getArgument(0))));
        when(repo.saveAndFlush(any())).thenAnswer(inv -> {
            WebhookEvent event = inv.getArgument(0);
            WebhookEvent previous = store.putIfAbsent(event.getIdempotencyKey(), event);
            if (previous != null && previous != event) {
                throw new DataIntegrityViolationException("duplicate idempotency key");
            }
            store.put(event.getIdempotencyKey(), event);
            return event;
        });
        when(repo.save(any())).thenAnswer(inv -> {
            WebhookEvent event = inv.getArgument(0);
            store.put(event.getIdempotencyKey(), event);
            return event;
        });
        return repo;
    }

    private static final class CountingCalculator extends CalculatorTool {
        private int executions;

        CountingCalculator() {
            super(new com.voiceos.provider.calculator.RealLocalCalculatorProvider());
        }

        @Override
        public com.voiceos.tool.core.ToolResult execute(Map<String, Object> params) {
            executions++;
            return super.execute(params);
        }
    }
}
