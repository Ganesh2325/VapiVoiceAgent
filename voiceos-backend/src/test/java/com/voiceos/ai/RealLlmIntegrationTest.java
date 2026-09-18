package com.voiceos.ai;

import com.voiceos.ai.gemini.GeminiLLMProvider;
import com.voiceos.ai.groq.GroqLLMProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Optional REAL LLM check. Skipped in normal CI unless VOICEOS_REAL_LLM_TEST=true
 * and a ChatModel bean is available. This is not a MockLLM test.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "voiceos.ai.mock=false"
})
@EnabledIfEnvironmentVariable(named = "VOICEOS_REAL_LLM_TEST", matches = "true")
class RealLlmIntegrationTest {

    @Autowired(required = false)
    private LLMProvider llmProvider;

    @Autowired(required = false)
    private java.util.List<ChatModel> chatModels;

    @Test
    void realProviderAnswersDependencyInjectionWithoutMockLabel() {
        assertNotNull(llmProvider, "LLMProvider bean is required for the optional real test");
        assertFalse(llmProvider instanceof com.voiceos.ai.mock.MockLLMProvider);
        assertFalse(llmProvider instanceof UnavailableLLMProvider);
        assertTrue(llmProvider instanceof GeminiLLMProvider || llmProvider instanceof GroqLLMProvider
                || (chatModels != null && !chatModels.isEmpty()));
        LLMResponse response = llmProvider.chat(LLMRequest.simple(
                "Answer briefly. Do not invent citations.",
                "What is dependency injection in Spring Boot?"
        ));
        assertNotNull(response);
        assertNotNull(response.content());
        assertFalse(response.content().isBlank());
        assertFalse(response.content().contains("[MOCK DATA]"));
        assertFalse("mock".equalsIgnoreCase(response.providerName()));
        assertNotNull(response.model());
    }
}
