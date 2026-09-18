package com.voiceos.ai.groq;

import com.voiceos.ai.LLMProvider;
import com.voiceos.ai.LLMRequest;
import com.voiceos.ai.LLMResponse;
import com.voiceos.ai.gemini.LLMProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

/**
 * Groq LLM provider.
 */
public class GroqLLMProvider implements LLMProvider {

    private static final Logger log = LoggerFactory.getLogger(GroqLLMProvider.class);

    private static final String PROVIDER_NAME = "groq";

    private final ChatModel chatModel;
    private final String modelName;

    public GroqLLMProvider(ChatModel chatModel, String modelName) {
        this.chatModel = chatModel;
        this.modelName = modelName;
    }

    @Override
    public LLMResponse chat(LLMRequest request) {
        long startMs = System.currentTimeMillis();
        log.debug("Sending chat request to Groq (model={})", modelName);

        try {
            List<org.springframework.ai.chat.messages.Message> messages = buildMessages(request);
            Prompt prompt = new Prompt(messages);

            ChatResponse response = chatModel.call(prompt);
            long latencyMs = System.currentTimeMillis() - startMs;

            String content = response.getResult().getOutput().getText();

            Integer inputTokens = null;
            Integer outputTokens = null;
            if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                var usage = response.getMetadata().getUsage();
                inputTokens = usage.getPromptTokens() != null
                        ? usage.getPromptTokens().intValue() : null;
                outputTokens = usage.getGenerationTokens() != null
                        ? usage.getGenerationTokens().intValue() : null;
            }

            log.debug("Groq response received in {}ms (tokens: in={}, out={})",
                    latencyMs, inputTokens, outputTokens);

            return new LLMResponse(
                    content,
                    PROVIDER_NAME,
                    modelName,
                    inputTokens,
                    outputTokens,
                    response.getResult().getMetadata().getFinishReason(),
                    latencyMs
            );

        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startMs;
            log.error("Groq chat request failed after {}ms: {}", latencyMs, e.getMessage());
            throw new LLMProviderException("Groq request failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Float> embed(String text) {
        // Groq does not currently offer embedding models
        // Fallback to Gemini embeddings handled at config level
        log.debug("Groq does not support embeddings — embedding will use Gemini");
        return List.of();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isAvailable() {
        return chatModel != null;
    }

    // ─── Private Helpers ─────────────────────────────────────────────────────

    private List<org.springframework.ai.chat.messages.Message> buildMessages(LLMRequest request) {
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();

        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            messages.add(new SystemMessage(request.systemPrompt()));
        }

        if (request.messageHistory() != null) {
            for (LLMRequest.LLMMessage histMsg : request.messageHistory()) {
                messages.add(switch (histMsg.role().toLowerCase()) {
                    case "user" -> new UserMessage(histMsg.content());
                    case "assistant" -> new AssistantMessage(histMsg.content());
                    default -> new UserMessage(histMsg.content());
                });
            }
        }

        if (request.userMessage() != null && !request.userMessage().isBlank()) {
            messages.add(new UserMessage(request.userMessage()));
        }

        return messages;
    }
}
