package com.voiceos.ai.mock;

import com.voiceos.ai.LLMProvider;
import com.voiceos.ai.LLMRequest;
import com.voiceos.ai.LLMResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mock LLM Provider for zero-cost local development and deterministic testing.
 *
 * <p>Produces realistic, context-aware responses and structured JSON outputs
 * without calling any external paid or rate-limited API.
 * All responses are explicitly marked with provider name {@code "mock"}.
 */
@Component
public class MockLLMProvider implements LLMProvider {

    private static final Logger log = LoggerFactory.getLogger(MockLLMProvider.class);
    private static final String PROVIDER_NAME = "mock";
    private final Random random = new Random();

    @Override
    public LLMResponse chat(LLMRequest request) {
        long startMs = System.currentTimeMillis();
        String userMsg = request.userMessage() != null ? request.userMessage().toLowerCase() : "";
        String systemPrompt = request.systemPrompt() != null ? request.systemPrompt().toLowerCase() : "";

        log.debug("[MOCK LLM] Processing prompt: user='{}'", userMsg);

        String generatedContent;

        // Check if orchestrator is asking for structured plan JSON
        if (systemPrompt.contains("orchestrator") || systemPrompt.contains("execution plan") || systemPrompt.contains("json")) {
            generatedContent = generateMockPlanJson(userMsg);
        } else if (userMsg.contains("bangalore") || userMsg.contains("trip") || userMsg.contains("travel")) {
            generatedContent = generateMockTravelResponse(userMsg);
        } else if (userMsg.contains("interview") || userMsg.contains("java") || userMsg.contains("spring")) {
            generatedContent = generateMockInterviewResponse(userMsg);
        } else if (userMsg.contains("task") || userMsg.contains("todo")) {
            generatedContent = generateMockTaskResponse(userMsg);
        } else if (userMsg.contains("email") || userMsg.contains("send")) {
            generatedContent = generateMockEmailResponse(userMsg);
        } else if (userMsg.contains("remember") || userMsg.contains("prefer")) {
            generatedContent = generateMockMemoryResponse(userMsg);
        } else if (userMsg.contains("research") || userMsg.contains("competitor")) {
            generatedContent = generateMockResearchResponse(userMsg);
        } else {
            generatedContent = generateGenericMockResponse(request.userMessage());
        }

        long latencyMs = System.currentTimeMillis() - startMs + 120; // simulate realistic fast inference
        int inputTokens = (userMsg.length() + systemPrompt.length()) / 4 + 10;
        int outputTokens = generatedContent.length() / 4;

        return new LLMResponse(
                generatedContent,
                PROVIDER_NAME,
                "mock-v1",
                inputTokens,
                outputTokens,
                "stop",
                latencyMs
        );
    }

    @Override
    public List<Float> embed(String text) {
        // Return deterministic 384-dimensional normalized mock vector for testing
        List<Float> vector = new ArrayList<>(384);
        int hash = text.hashCode();
        for (int i = 0; i < 384; i++) {
            vector.add((float) Math.sin(hash + i));
        }
        return vector;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    // ─── Specialized Mock Generators ─────────────────────────────────────────

    private String generateMockPlanJson(String prompt) {
        if (prompt.contains("trip") || prompt.contains("bangalore")) {
            return """
            {
              "goal": "Plan trip to Bangalore for next Friday",
              "steps": [
                {"stepIndex": 0, "agentName": "CalendarAgent", "action": "check_availability", "description": "Check user schedule for next Friday"},
                {"stepIndex": 1, "agentName": "TravelAgent", "action": "search_flights", "description": "Search available flights to Bangalore"},
                {"stepIndex": 2, "agentName": "TravelAgent", "action": "search_hotels", "description": "Search hotels near Indiranagar, Bangalore"},
                {"stepIndex": 3, "agentName": "FinanceAgent", "action": "estimate_budget", "description": "Calculate estimated total trip cost"}
              ]
            }
            """;
        }
        if (prompt.contains("email")) {
            return """
            {
              "goal": "Prepare and send email",
              "steps": [
                {"stepIndex": 0, "agentName": "EmailAgent", "action": "draft_email", "description": "Draft email message for user review"}
              ]
            }
            """;
        }
        return """
        {
          "goal": "Process user request",
          "steps": [
            {"stepIndex": 0, "agentName": "ConversationAgent", "action": "respond", "description": "Generate helpful response to user"}
          ]
        }
        """;
    }

    private String generateMockTravelResponse(String prompt) {
        return """
        [MOCK DATA] Here is your customized Bangalore itinerary for next Friday:

        ✈️ **Flight Options:**
        - Indigo 6E-2134: 07:15 AM - 09:30 AM ($68)
        - Air India AI-508: 09:45 AM - 12:00 PM ($75)

        🏨 **Hotel Recommendations:**
        - The Oberoi, MG Road ($140/night) - 4.8★
        - Grand Mercure, Koramangala ($95/night) - 4.6★

        💰 **Estimated Budget:** $215 - $280 total

        Would you like me to prepare a booking request for your approval?
        """;
    }

    private String generateMockInterviewResponse(String prompt) {
        return """
        [MOCK DATA] Excellent! Let's conduct a technical interview for a **Senior Java & Spring Boot Engineer** role.

        **Question 1:**
        "In Spring Boot 3.x with Java 21, how do Virtual Threads (Project Loom) change the way we handle concurrent I/O requests compared to traditional WebFlux reactive streams? When would you still choose WebFlux over Virtual Threads?"

        Take your time to answer, and I will evaluate your response!
        """;
    }

    private String generateMockTaskResponse(String prompt) {
        return "[MOCK DATA] Task created successfully. It has been added to your VoiceOS task list.";
    }

    private String generateMockEmailResponse(String prompt) {
        return """
        [MOCK DATA] I have prepared the email draft.

        ⚠️ **Action Requires Approval:**
        - **Action:** Send Email
        - **Recipient:** John Doe (john@example.com)
        - **Subject:** Project Update
        - **Risk Level:** HIGH

        Please approve or reject this action in your Approvals tab.
        """;
    }

    private String generateMockMemoryResponse(String prompt) {
        return "[MOCK DATA] I've committed this preference to your long-term memory. I'll remember this for our future interactions.";
    }

    private String generateMockResearchResponse(String prompt) {
        return """
        [MOCK DATA] **Research Summary:**
        1. **Market Overview:** Key competitors include established cloud voice solutions and specialized open-source agent frameworks.
        2. **Key Differentiator:** VoiceOS provides unified multi-agent delegation with zero mandatory paid dependencies.
        3. **Recommendation:** Focus on developer experience and self-hosted LiveKit integration.
        """;
    }

    private String generateGenericMockResponse(String userMsg) {
        return "[MOCK DATA] I processed your request: \"" + (userMsg != null ? userMsg : "") + "\". How else can I assist you?";
    }
}
