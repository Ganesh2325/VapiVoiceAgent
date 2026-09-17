package com.voiceos.action;

import com.voiceos.action.audit.ActionTimelineDto;
import com.voiceos.action.audit.ActionTraceService;
import com.voiceos.action.engine.ActionEngine;
import com.voiceos.action.engine.ResultVerifier;
import com.voiceos.action.model.Action;
import com.voiceos.action.model.ActionCommand;
import com.voiceos.action.model.ActionExecutionResult;
import com.voiceos.action.model.ActionStatus;
import com.voiceos.action.model.ActionStepType;
import com.voiceos.action.model.AuditEventType;
import com.voiceos.action.store.InMemoryActionStore;
import com.voiceos.action.store.InMemoryActionStepStore;
import com.voiceos.action.store.InMemoryAuditEventStore;
import com.voiceos.agent.core.AgentRegistry;
import com.voiceos.agent.core.RegistryAgentSelector;
import com.voiceos.agent.impl.UtilityAgent;
import com.voiceos.service.PolicyEngine;
import com.voiceos.tool.core.ToolRegistry;
import com.voiceos.tool.impl.CalculatorTool;
import com.voiceos.tool.impl.ForbiddenTool;
import com.voiceos.tool.impl.ProbeFailTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionEnginePipelineTest {

    private static final UUID USER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private ActionEngine actionEngine;
    private ActionTraceService traceService;
    private ForbiddenTool forbiddenTool;
    private ProbeFailTool probeFailTool;
    private CountingCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CountingCalculator();
        forbiddenTool = new ForbiddenTool();
        probeFailTool = new ProbeFailTool(new com.voiceos.provider.probe.FailingProbeProvider());
        ToolRegistry tools = new ToolRegistry(List.of(calculator, forbiddenTool, probeFailTool));
        UtilityAgent utilityAgent = new UtilityAgent(tools);
        traceService = new ActionTraceService(new InMemoryActionStepStore(), new InMemoryAuditEventStore());
        actionEngine = new ActionEngine(
                new InMemoryActionStore(),
                new PolicyEngine(),
                new RegistryAgentSelector(new AgentRegistry(List.of(utilityAgent))),
                tools,
                new ResultVerifier(),
                null, null, null, null, null,
                traceService
        );
    }

    @Test
    void calculatorExecutesReallyAndIsNotHardcoded() {
        ActionExecutionResult result = actionEngine.execute(new ActionCommand(
                USER, null, "call-1", "req-1", null, "calc-1", "TEST",
                "Calculate 125 multiplied by 24", "calculator",
                Map.of("expression", "125 * 24")
        ));
        assertEquals(ActionStatus.COMPLETED.name(), result.status());
        assertEquals("UtilityAgent", result.agent());
        assertEquals("calculator", result.tool());
        assertEquals("REAL", result.providerMode());
        assertEquals("RealLocalCalculatorProvider", result.provider());
        assertEquals("3000.00", result.result());
        assertTrue(result.success());
        assertEquals(1, calculator.executions);

        ActionTimelineDto timeline = actionEngine.timelineForUser(result.actionId(), USER).orElseThrow();
        assertEquals("ALLOW", timeline.policyDecision());
        assertEquals(Boolean.TRUE, timeline.verificationPassed());
        List<String> eventTypes = timeline.events().stream().map(ActionTimelineDto.EventView::type).toList();
        assertTrue(eventTypes.contains(AuditEventType.ACTION_CREATED.name()));
        assertTrue(eventTypes.contains(AuditEventType.POLICY_EVALUATED.name()));
        assertTrue(eventTypes.contains(AuditEventType.TOOL_STARTED.name()));
        assertTrue(eventTypes.contains(AuditEventType.TOOL_SUCCEEDED.name()));
        assertTrue(eventTypes.contains(AuditEventType.PROVIDER_SUCCEEDED.name()));
        assertTrue(eventTypes.contains(AuditEventType.VERIFICATION_PASSED.name()));
        assertTrue(eventTypes.contains(AuditEventType.ACTION_COMPLETED.name()));
        assertFalse(eventTypes.contains(AuditEventType.TOOL_FAILED.name()));
        assertEquals(List.of(
                ActionStepType.RECEIVED.name(),
                ActionStepType.VALIDATING.name(),
                ActionStepType.AUTHORIZING.name(),
                ActionStepType.PLANNING.name(),
                ActionStepType.POLICY_CHECK.name(),
                ActionStepType.TOOL_EXECUTION.name(),
                ActionStepType.VERIFICATION.name(),
                ActionStepType.COMPLETION.name()
        ), timeline.steps().stream().map(ActionTimelineDto.StepView::stepType).toList());
        for (int i = 0; i < timeline.steps().size(); i++) {
            assertEquals(i + 1, timeline.steps().get(i).sequenceNo());
            assertEquals("SUCCEEDED", timeline.steps().get(i).status());
        }
    }

    @Test
    void policyDenyPreventsToolExecution() {
        ActionExecutionResult result = actionEngine.execute(new ActionCommand(
                USER, null, null, "req-deny", null, "deny-1", "TEST",
                "forbidden", "forbidden", Map.of()
        ));
        assertEquals(ActionStatus.REJECTED.name(), result.status());
        assertFalse(result.success());
        assertEquals(0, forbiddenTool.executionCount());
        ActionTimelineDto timeline = actionEngine.timelineForUser(result.actionId(), USER).orElseThrow();
        List<String> eventTypes = timeline.events().stream().map(ActionTimelineDto.EventView::type).toList();
        assertTrue(eventTypes.contains(AuditEventType.POLICY_EVALUATED.name()));
        assertTrue(eventTypes.contains(AuditEventType.POLICY_DENIED.name()));
        assertTrue(eventTypes.contains(AuditEventType.ACTION_REJECTED.name()));
        assertFalse(eventTypes.contains(AuditEventType.TOOL_STARTED.name()));
        assertFalse(eventTypes.contains(AuditEventType.TOOL_SUCCEEDED.name()));
        assertEquals("DENY", timeline.policyDecision());
    }

    @Test
    void providerFailureYieldsFailedNotCompleted() {
        ActionExecutionResult result = actionEngine.execute(new ActionCommand(
                USER, null, null, "req-fail", null, "fail-1", "TEST",
                "probe_fail", "probe_fail", Map.of()
        ));
        assertEquals(ActionStatus.FAILED.name(), result.status());
        assertFalse(result.success());
        assertNotEquals(ActionStatus.COMPLETED.name(), result.status());
        assertEquals(1, probeFailTool.executionCount());
        ActionTimelineDto timeline = actionEngine.timelineForUser(result.actionId(), USER).orElseThrow();
        List<String> eventTypes = timeline.events().stream().map(ActionTimelineDto.EventView::type).toList();
        assertTrue(eventTypes.contains(AuditEventType.TOOL_STARTED.name()));
        assertTrue(eventTypes.contains(AuditEventType.PROVIDER_FAILED.name()));
        assertTrue(eventTypes.contains(AuditEventType.TOOL_FAILED.name()));
        assertTrue(eventTypes.contains(AuditEventType.VERIFICATION_FAILED.name()));
        assertTrue(eventTypes.contains(AuditEventType.ACTION_FAILED.name()));
        assertFalse(eventTypes.contains(AuditEventType.TOOL_SUCCEEDED.name()));
        assertFalse(eventTypes.contains(AuditEventType.VERIFICATION_PASSED.name()));
        assertEquals(Boolean.FALSE, timeline.verificationPassed());
    }

    @Test
    void missingUserIsRejected() {
        ActionExecutionResult result = actionEngine.execute(new ActionCommand(
                null, null, "call-x", "req-x", null, "unbound-1", "VAPI",
                "calculator", "calculator", Map.of("expression", "1 + 1")
        ));
        assertEquals(ActionStatus.REJECTED.name(), result.status());
        assertEquals(0, calculator.executions);
        var events = traceService.eventsFor(result.actionId());
        assertTrue(events.stream().anyMatch(e -> e.getEventType() == AuditEventType.IDENTITY_UNRESOLVED));
        assertTrue(events.stream().anyMatch(e -> e.getEventType() == AuditEventType.ACTION_REJECTED));
        assertTrue(events.stream().noneMatch(e -> e.getEventType() == AuditEventType.TOOL_STARTED));
    }

    @Test
    void duplicateIdempotencyKeyDoesNotExecuteTwice() {
        ActionCommand command = new ActionCommand(
                USER, null, "call-dup", "req-dup", null, "same-key", "TEST",
                "calculator", "calculator", Map.of("expression", "10 + 5")
        );
        ActionExecutionResult first = actionEngine.execute(command);
        ActionExecutionResult second = actionEngine.execute(command);
        assertEquals("15.00", first.result());
        assertEquals("15.00", second.result());
        assertEquals(first.actionId(), second.actionId());
        assertEquals(1, calculator.executions);
        assertEquals(
                traceService.eventsFor(first.actionId()).size(),
                traceService.eventsFor(second.actionId()).size()
        );
    }

    @Test
    void secretsAreRedactedFromAuditMetadata() {
        ActionExecutionResult result = actionEngine.execute(new ActionCommand(
                USER, null, null, "req-secret", null, "secret-1", "TEST",
                "calculator", "calculator",
                Map.of("expression", "1 + 1", "jwt", "aaa.bbb.ccc", "password", "hunter2", "authorization", "Bearer abc")
        ));
        assertEquals(ActionStatus.COMPLETED.name(), result.status());
        Action action = actionEngine.getAction(result.actionId());
        assertEquals("[REDACTED]", action.getPayload().get("jwt"));
        assertEquals("[REDACTED]", action.getPayload().get("password"));
        assertEquals("[REDACTED]", action.getPayload().get("authorization"));
        assertEquals("1 + 1", action.getPayload().get("expression"));
        String blob = traceService.eventsFor(result.actionId()).stream()
                .map(e -> String.valueOf(e.getMetadata()))
                .reduce("", (a, b) -> a + b)
                + String.valueOf(action.getPayload());
        assertFalse(blob.contains("aaa.bbb.ccc"));
        assertFalse(blob.contains("hunter2"));
        assertFalse(blob.contains("Bearer abc"));
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
