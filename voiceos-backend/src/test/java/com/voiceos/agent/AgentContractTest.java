package com.voiceos.agent;

import com.voiceos.action.audit.ActionTimelineDto;
import com.voiceos.action.audit.ActionTraceService;
import com.voiceos.action.engine.ActionEngine;
import com.voiceos.action.engine.ResultVerifier;
import com.voiceos.action.model.ActionCommand;
import com.voiceos.action.model.ActionExecutionResult;
import com.voiceos.action.model.ActionStatus;
import com.voiceos.action.model.AuditEventType;
import com.voiceos.action.store.InMemoryActionStore;
import com.voiceos.action.store.InMemoryActionStepStore;
import com.voiceos.action.store.InMemoryAuditEventStore;
import com.voiceos.agent.core.Agent;
import com.voiceos.agent.core.AgentCapability;
import com.voiceos.agent.core.AgentContext;
import com.voiceos.agent.core.AgentOutcome;
import com.voiceos.agent.core.AgentRegistry;
import com.voiceos.agent.core.AgentRequest;
import com.voiceos.agent.core.AgentResult;
import com.voiceos.agent.core.AgentSelector;
import com.voiceos.agent.core.RegistryAgentSelector;
import com.voiceos.agent.impl.ApprovalAgent;
import com.voiceos.agent.impl.GeneralQueryAgent;
import com.voiceos.agent.impl.FinanceAgent;
import com.voiceos.agent.impl.TravelAgent;
import com.voiceos.agent.impl.UtilityAgent;
import com.voiceos.agent.impl.WorkflowAgent;
import com.voiceos.ai.mock.MockLLMProvider;
import com.voiceos.service.PolicyEngine;
import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.ToolRegistry;
import com.voiceos.tool.core.ToolResult;
import com.voiceos.tool.impl.CalculatorTool;
import com.voiceos.tool.impl.ForbiddenTool;
import com.voiceos.tool.impl.SearchTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentContractTest {

    private static final UUID USER = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private CountingCalculator calculator;
    private CountingSearch search;
    private ForbiddenTool forbiddenTool;
    private ToolRegistry tools;
    private AgentRegistry registry;
    private AgentSelector selector;
    private ActionEngine actionEngine;

    @BeforeEach
    void setUp() {
        calculator = new CountingCalculator();
        search = new CountingSearch();
        forbiddenTool = new ForbiddenTool();
        tools = new ToolRegistry(List.of(calculator, search, forbiddenTool));
        UtilityAgent utility = new UtilityAgent(tools);
        TravelAgent travel = new TravelAgent(tools);
        FinanceAgent finance = new FinanceAgent(tools);
        GeneralQueryAgent conversation = new GeneralQueryAgent(new MockLLMProvider(), null);
        registry = new AgentRegistry(List.of(utility, travel, finance, conversation,
                new ApprovalAgent(), new WorkflowAgent(null)));
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
    void agentsDeclareCapabilitiesAndOwnedTools() {
        Agent utility = registry.getAgent("UtilityAgent").orElseThrow();
        assertEquals(Set.of(AgentCapability.CALCULATOR, AgentCapability.UTILITY), utility.capabilities());
        assertTrue(utility.ownsTool("calculator"));
        assertTrue(registry.findByCapability(AgentCapability.TRAVEL).stream()
                .anyMatch(agent -> "TravelAgent".equals(agent.getName())));
        assertTrue(registry.findByOwnedTool("calculator").orElseThrow().getName().equals("UtilityAgent"));
    }

    @Test
    void duplicateAgentNamesFailFast() {
        Agent first = new NamedStub("DupAgent", 10, Set.of());
        Agent second = new NamedStub("DupAgent", 20, Set.of());
        assertThrows(IllegalStateException.class, () -> new AgentRegistry(List.of(first, second)));
    }

    @Test
    void selectionIsDeterministicAndUsesLowerPriorityThenName() {
        Agent alpha = new NamedStub("AlphaAgent", 50, Set.of(AgentCapability.SUPPORT));
        Agent beta = new NamedStub("BetaAgent", 50, Set.of(AgentCapability.SUPPORT));
        Agent win = new NamedStub("WinAgent", 5, Set.of(AgentCapability.SUPPORT));
        AgentRegistry local = new AgentRegistry(List.of(beta, alpha, win));
        List<String> names = local.getAllAgents().stream().map(Agent::getName).toList();
        assertEquals(List.of("WinAgent", "AlphaAgent", "BetaAgent"), names);
        assertEquals("WinAgent", local.findByCapability(AgentCapability.SUPPORT).get(0).getName());
        assertEquals("AlphaAgent", local.getAllAgents().stream()
                .filter(agent -> agent.getPriority() == 50)
                .findFirst().orElseThrow().getName());
    }

    @Test
    void calculatorRequestSelectsUtilityAgentNotTravel() {
        AgentRequest request = new AgentRequest(USER, null, "r1", null, null,
                "125 multiplied by 24", "calculator", Map.of("expression", "125 * 24"), null);
        assertEquals("UtilityAgent", selector.select(request).orElseThrow().getName());
    }

    @Test
    void specializedTravelSelectionAndConversationFallback() {
        AgentRequest travel = new AgentRequest(USER, null, "r2", null, null,
                "Book me a flight", null, Map.of(), null);
        assertEquals("TravelAgent", selector.select(travel).orElseThrow().getName());

        AgentRequest chat = new AgentRequest(USER, null, "r3", null, null,
                "Hello, how are you today?", null, Map.of(), null);
        assertEquals("GeneralQueryAgent", selector.select(chat).orElseThrow().getName());
        assertTrue(registry.findHandler(chat).isEmpty());
    }

    @Test
    void unknownCapabilityAndUnknownTool() {
        assertTrue(registry.findByCapability(null).isEmpty());
        assertTrue(registry.findByOwnedTool("no_such_tool").isEmpty());
        ActionExecutionResult result = actionEngine.execute(new ActionCommand(
                USER, null, null, "req-unknown", null, "unk-1", "TEST",
                "unknown", "no_such_tool", Map.of()
        ));
        assertEquals(ActionStatus.FAILED.name(), result.status());
        assertTrue(result.error().contains("Unknown tool"));
    }

    @Test
    void needsInformationDoesNotRunProvider() {
        ActionExecutionResult result = actionEngine.execute(new ActionCommand(
                USER, null, null, "req-flight", null, "flight-1", "TEST",
                "Book me a flight", null, Map.of()
        ));
        assertEquals(ActionStatus.WAITING_FOR_USER.name(), result.status());
        assertEquals("TravelAgent", result.agent());
        assertNotCompleted(result);
        assertEquals(0, search.executions);
        assertTrue(result.error().toLowerCase().contains("origin"));
        ActionTimelineDto timeline = actionEngine.timelineForUser(result.actionId(), USER).orElseThrow();
        List<String> types = timeline.events().stream().map(ActionTimelineDto.EventView::type).toList();
        assertTrue(types.contains(AuditEventType.AGENT_SELECTED.name()));
        assertTrue(types.contains(AuditEventType.AGENT_STARTED.name()));
        assertTrue(types.contains(AuditEventType.AGENT_SUCCEEDED.name()));
        assertFalse(types.contains(AuditEventType.TOOL_STARTED.name()));
        assertFalse(types.contains(AuditEventType.TOOL_SUCCEEDED.name()));
        assertFalse(types.contains(AuditEventType.PROVIDER_STARTED.name()));
        @SuppressWarnings("unchecked")
        List<String> missing = (List<String>) actionEngine.getAction(result.actionId()).getPayload().get("missingFields");
        assertTrue(missing.contains("origin"));
        assertTrue(missing.contains("destination"));
        assertTrue(missing.contains("travelDate"));
    }

    @Test
    void financeAgentDoesNotFabricateBudget() {
        ActionExecutionResult result = actionEngine.execute(new ActionCommand(
                USER, null, null, "req-budget", null, "budget-1", "TEST",
                "Show my expense breakdown", null, Map.of()
        ));
        assertEquals("FinanceAgent", result.agent());
        assertEquals(ActionStatus.WAITING_FOR_USER.name(), result.status());
        assertEquals(0, calculator.executions);
        assertFalse(String.valueOf(result.result()).contains("258"));
        assertFalse(String.valueOf(result.error()).contains("258"));
        assertTrue(result.error().toLowerCase().contains("expression"));
    }

    @Test
    void incompleteAgentsRemainHonest() {
        AgentResult approval = new ApprovalAgent().execute(AgentContext.of(USER, USER, "show approval queue"));
        assertEquals(AgentOutcome.NOT_IMPLEMENTED, approval.outcome());
        assertFalse(approval.success());

        AgentResult workflow = new WorkflowAgent(null)
                .execute(AgentContext.of(USER, USER, "schedule workflow tomorrow"));
        assertEquals(AgentOutcome.NOT_IMPLEMENTED, workflow.outcome());
        assertFalse(workflow.success());
    }

    @Test
    void agentCannotBypassPolicyEngine() {
        ActionExecutionResult result = actionEngine.execute(new ActionCommand(
                USER, null, null, "req-forbid", null, "forbid-1", "TEST",
                "forbidden", "forbidden", Map.of()
        ));
        assertEquals(ActionStatus.REJECTED.name(), result.status());
        assertEquals("UtilityAgent", result.agent());
        assertEquals(0, forbiddenTool.executionCount());
        ActionTimelineDto timeline = actionEngine.timelineForUser(result.actionId(), USER).orElseThrow();
        assertEquals("DENY", timeline.policyDecision());
        assertFalse(timeline.events().stream().anyMatch(e -> AuditEventType.TOOL_STARTED.name().equals(e.type())));
    }

    @Test
    void clientAgentNameDoesNotOverrideRouting() {
        ActionExecutionResult result = actionEngine.execute(new ActionCommand(
                USER, null, null, "req-hijack", null, "hijack-1", "TEST",
                "125 multiplied by 24", "calculator",
                Map.of("expression", "125 * 24", "agent", "FinanceAgent", "agentName", "TravelAgent",
                        "provider", "MockTravelProvider")
        ));
        assertEquals(ActionStatus.COMPLETED.name(), result.status());
        assertEquals("UtilityAgent", result.agent());
        assertEquals("3000.00", result.result());
        assertEquals("RealLocalCalculatorProvider", result.provider());
        assertEquals("REAL", result.providerMode());
        assertEquals(1, calculator.executions);
    }

    @Test
    void userIsolationHidesAnotherUsersAction() {
        UUID other = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        ActionExecutionResult created = actionEngine.execute(new ActionCommand(
                USER, null, null, "req-iso", null, "iso-1", "TEST",
                "calculator", "calculator", Map.of("expression", "2 + 2")
        ));
        assertTrue(actionEngine.getActionForUser(created.actionId(), USER).isPresent());
        assertTrue(actionEngine.getActionForUser(created.actionId(), other).isEmpty());
        assertTrue(actionEngine.timelineForUser(created.actionId(), other).isEmpty());
    }

    @Test
    void structuredOutcomesCoverConfirmationRejectionAndFailure() {
        AgentResult needs = AgentResult.needsInformation("TravelAgent", "missing", List.of("origin"), 1);
        assertEquals(AgentOutcome.NEEDS_INFORMATION, needs.outcome());
        AgentResult confirm = AgentResult.approvalRequired("EmailAgent", "confirm",
                new AgentResult.ApprovalRequestData("SEND_EMAIL", "send", "HIGH", Map.of()), 1);
        assertEquals(AgentOutcome.NEEDS_USER_CONFIRMATION, confirm.outcome());
        AgentResult rejected = AgentResult.rejected("SecurityAgent", "no", 1);
        assertEquals(AgentOutcome.REJECTED, rejected.outcome());
        AgentResult failed = AgentResult.failure("UtilityAgent", "boom", 1);
        assertEquals(AgentOutcome.FAILED, failed.outcome());
        AgentResult execute = AgentResult.withTools("UtilityAgent", "3000.00",
                List.of(ToolResult.success("3000.00", 1)), 1);
        assertEquals(AgentOutcome.EXECUTE, execute.outcome());
    }

    @Test
    void conversationAgentDoesNotExecuteForeignTools() {
        GeneralQueryAgent conversation = new GeneralQueryAgent(new MockLLMProvider(), null);
        AgentResult result = conversation.execute(new AgentRequest(
                USER, null, null, null, null, "hi", "calculator", Map.of(), null));
        assertEquals(AgentOutcome.FAILED, result.outcome());
        assertEquals("UNSUPPORTED_REQUEST", result.errorCode());
        assertTrue(result.toolExecutions().isEmpty());
    }

    @Test
    void agentRequestStripsSecretsAndClientRouting() {
        AgentRequest request = new AgentRequest(USER, null, "r", "call", null, "hi", "calculator",
                Map.of("expression", "1+1", "agent", "FinanceAgent", "jwt", "aaa.bbb.ccc", "password", "x"),
                "en");
        assertFalse(request.parameters().containsKey("agent"));
        assertFalse(request.parameters().containsKey("jwt"));
        assertFalse(request.parameters().containsKey("password"));
        assertEquals("1+1", request.parameterAsString("expression"));
    }

    private static void assertNotCompleted(ActionExecutionResult result) {
        assertFalse(result.success());
        assertNotEquals(ActionStatus.COMPLETED.name(), result.status());
    }

    private static void assertNotEquals(String unexpected, String actual) {
        assertFalse(unexpected.equals(actual), () -> "did not expect " + unexpected);
    }

    private static final class CountingCalculator extends CalculatorTool {
        private int executions;

        CountingCalculator() {
            super(new com.voiceos.provider.calculator.RealLocalCalculatorProvider());
        }

        @Override
        public ToolResult execute(Map<String, Object> params) {
            executions++;
            return super.execute(params);
        }
    }

    private static final class CountingSearch extends SearchTool {
        private int executions;

        @Override
        public ToolResult execute(Map<String, Object> params) {
            executions++;
            return super.execute(params);
        }
    }

    private static final class NamedStub implements Agent {
        private final String name;
        private final int priority;
        private final Set<AgentCapability> capabilities;

        NamedStub(String name, int priority, Set<AgentCapability> capabilities) {
            this.name = name;
            this.priority = priority;
            this.capabilities = capabilities;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return name;
        }

        @Override
        public boolean canHandle(AgentContext context) {
            return true;
        }

        @Override
        public AgentResult execute(AgentContext context) {
            return AgentResult.success(name, "ok", 0);
        }

        @Override
        public List<Tool> getTools() {
            return List.of();
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public Set<AgentCapability> capabilities() {
            return capabilities;
        }
    }
}
