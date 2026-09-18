package com.voiceos;

import com.voiceos.agent.core.AgentContext;
import com.voiceos.agent.core.AgentRegistry;
import com.voiceos.agent.impl.GeneralQueryAgent;
import com.voiceos.agent.impl.TravelAgent;
import com.voiceos.ai.mock.MockLLMProvider;
import com.voiceos.tool.core.ToolRegistry;
import com.voiceos.tool.impl.CalculatorTool;
import com.voiceos.tool.impl.SearchTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentRegistryTest {

    private AgentRegistry agentRegistry;
    private ToolRegistry toolRegistry;

    @BeforeEach
    void setUp() {
        toolRegistry = new ToolRegistry(List.of(
                new CalculatorTool(new com.voiceos.provider.calculator.RealLocalCalculatorProvider()),
                new SearchTool()));

        GeneralQueryAgent general = new GeneralQueryAgent(new MockLLMProvider(), null);
        TravelAgent travelAgent = new TravelAgent(toolRegistry);

        agentRegistry = new AgentRegistry(List.of(general, travelAgent));
    }

    @Test
    void testAgentDiscovery() {
        assertEquals(2, agentRegistry.getAllAgents().size());
        assertTrue(agentRegistry.hasAgent("TravelAgent"));
        assertTrue(agentRegistry.hasAgent("GeneralQueryAgent"));
    }

    @Test
    void testIntentRoutingToSpecializedAgent() {
        AgentContext context = AgentContext.of(
                UUID.randomUUID(), UUID.randomUUID(), "Plan a trip to Bangalore for next Friday"
        );

        Optional<com.voiceos.agent.core.Agent> handler = agentRegistry.findHandler(context);
        assertTrue(handler.isPresent());
        assertEquals("TravelAgent", handler.get().getName());
    }

    @Test
    void testFallbackWhenNoSpecializedMatch() {
        AgentContext context = AgentContext.of(
                UUID.randomUUID(), UUID.randomUUID(), "Hello, how are you today?"
        );

        Optional<com.voiceos.agent.core.Agent> handler = agentRegistry.findHandler(context);
        assertTrue(handler.isEmpty());
    }
}
