package com.voiceos.ai;

import com.voiceos.ai.gemini.GeminiLLMProvider;
import com.voiceos.ai.groq.GroqLLMProvider;
import com.voiceos.config.VoiceOsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * LLM Provider Configuration.
 *
 * <p>Spring AI 1.0.0 auto-configures ChatModel beans when starters are present.
 * We inject those auto-configured beans with qualifiers and wrap them in our
 * provider abstraction, selecting the active one based on {@code voiceos.ai.provider}.
 *
 * <p>Bean names from Spring AI auto-configuration:
 * <ul>
 *   <li>Gemini: {@code googleGenAiChatModel}</li>
 *   <li>OpenAI/Groq: {@code openAiChatModel}</li>
 * </ul>
 */
@Configuration
public class LLMProviderConfig {

    private static final Logger log = LoggerFactory.getLogger(LLMProviderConfig.class);

    private final VoiceOsProperties properties;

    @Value("${spring.ai.google.genai.chat.options.model:gemini-2.0-flash}")
    private String geminiModel;

    @Value("${spring.ai.openai.chat.options.model:llama-3.3-70b-versatile}")
    private String groqModel;

    public LLMProviderConfig(VoiceOsProperties properties) {
        this.properties = properties;
    }

    /**
     * Primary LLMProvider bean — wraps the Spring AI ChatModel selected by config.
     * Uses the auto-configured ChatModel beans from Spring AI starters.
     */
    @Bean
    @Primary
    public LLMProvider llmProvider(
            java.util.List<ChatModel> chatModels,
            com.voiceos.ai.mock.MockLLMProvider mockLLMProvider
    ) {
        if (properties.ai().mock()) {
            log.info("Mock mode enabled (voiceos.ai.mock=true) — using MockLLMProvider");
            return mockLLMProvider;
        }

        String provider = properties.ai().provider();
        log.info("Configuring LLM provider: {}", provider);

        if ("mock".equalsIgnoreCase(provider)) {
            return mockLLMProvider;
        }

        ChatModel chatModel = null;
        if (chatModels != null && !chatModels.isEmpty()) {
            if ("gemini".equalsIgnoreCase(provider)) {
                chatModel = chatModels.stream()
                        .filter(c -> c.getClass().getName().toLowerCase().contains("gemini") || c.getClass().getName().toLowerCase().contains("vertex"))
                        .findFirst()
                        .orElse(chatModels.get(0));
            } else if ("groq".equalsIgnoreCase(provider)) {
                chatModel = chatModels.stream()
                        .filter(c -> c.getClass().getName().toLowerCase().contains("openai"))
                        .findFirst()
                        .orElse(chatModels.get(0));
            } else {
                chatModel = chatModels.get(0);
            }
        }

        if (chatModel == null) {
            log.warn("No Spring AI ChatModel bean available for provider '{}'. LLM is UNAVAILABLE (no silent MOCK fallback).", provider);
            return new UnavailableLLMProvider(provider);
        }

        return switch (provider.toLowerCase()) {
            case "gemini" -> new GeminiLLMProvider(chatModel, geminiModel);
            case "groq" -> new GroqLLMProvider(chatModel, groqModel);
            default -> {
                log.warn("Unknown AI provider '{}'. LLM is UNAVAILABLE (no silent MOCK fallback).", provider);
                yield new UnavailableLLMProvider(provider);
            }
        };
    }

    @Bean("fallbackLLMProvider")
    public LLMProvider fallbackLLMProvider(
            com.voiceos.ai.mock.MockLLMProvider mockLLMProvider
    ) {
        return mockLLMProvider;
    }
}
