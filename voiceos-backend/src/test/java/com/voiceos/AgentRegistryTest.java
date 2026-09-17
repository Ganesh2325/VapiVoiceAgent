package com.voiceos;

import com.voiceos.agent.core.Agent;
import com.voiceos.agent.core.AgentContext;
import com.voiceos.agent.core.AgentRegistry;
import com.voiceos.agent.impl.ConversationAgent;
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

import static org.junit.jupiter.api.Assertions.*;

class AgentRegistryTest {

    private AgentRegistry agentRegistry;
    private MockLLMProvider mockLlmProvider;
    private ToolRegistry toolRegistry;

    @BeforeEach
    void setUp() {
        mockLlmProvider = new MockLLMProvider();
        toolRegistry = new ToolRegistry(List.of(new CalculatorTool(new com.voiceos.provider.calculator.RealLocalCalculatorProvider()), new SearchTool()));

        ConversationAgent convAgent = new ConversationAgent(mockLlmProvider);
        TravelAgent travelAgent = new TravelAgent(toolRegistry);

        agentRegistry = new AgentRegistry(List.of(convAgent, travelAgent));
    }

    @Test
    void testAgentDiscovery() {
        assertEquals(2, agentRegistry.getAllAgents().size());
        assertTrue(agentRegistry.hasAgent("TravelAgent"));
        assertTrue(agentRegistry.hasAgent("ConversationAgent"));
    }

    @Test
    void testIntentRoutingToSpecializedAgent() {
        AgentContext context = AgentContext.of(
                UUID.randomUUID(), UUID.randomUUID(), "Plan a trip to Bangalore for next Friday"
        );

        Optional<Agent> handler = agentRegistry.findHandler(context);
        assertTrue(handler.isPresent());
        assertEquals("TravelAgent", handler.get().getName());
    }

    @Test
    void testFallbackWhenNoSpecializedMatch() {
        AgentContext context = AgentContext.of(
                UUID.randomUUID(), UUID.randomUUID(), "Hello, how are you today?"
        );

        Optional<Agent> handler = agentRegistry.findHandler(context);
        // Specialized handler should be empty so orchestrator uses ConversationAgent
        assertTrue(handler.isEmpty());
    }
}
