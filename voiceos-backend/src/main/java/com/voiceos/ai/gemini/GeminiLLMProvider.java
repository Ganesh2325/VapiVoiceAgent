package com.voiceos.ai.gemini;

import com.voiceos.ai.LLMProvider;
import com.voiceos.ai.LLMRequest;
import com.voiceos.ai.LLMResponse;
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
 * Google Gemini LLM provider.
 */
public class GeminiLLMProvider implements LLMProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiLLMProvider.class);

    private static final String PROVIDER_NAME = "gemini";

    private final ChatModel chatModel;
    private final String modelName;

    public GeminiLLMProvider(ChatModel chatModel, String modelName) {
        this.chatModel = chatModel;
        this.modelName = modelName;
    }

    @Override
    public LLMResponse chat(LLMRequest request) {
        long startMs = System.currentTimeMillis();
        log.debug("Sending chat request to Gemini (model={})", modelName);

        try {
            List<org.springframework.ai.chat.messages.Message> messages = buildMessages(request);
            Prompt prompt = new Prompt(messages);

            ChatResponse response = chatModel.call(prompt);
            long latencyMs = System.currentTimeMillis() - startMs;

            String content = response.getResult().getOutput().getText();

            // Extract token usage if available
            Integer inputTokens = null;
            Integer outputTokens = null;
            if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                var usage = response.getMetadata().getUsage();
                inputTokens = usage.getPromptTokens() != null
                        ? usage.getPromptTokens().intValue() : null;
                outputTokens = usage.getGenerationTokens() != null
                        ? usage.getGenerationTokens().intValue() : null;
            }

            log.debug("Gemini response received in {}ms (tokens: in={}, out={})",
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
            log.error("Gemini chat request failed after {}ms: {}", latencyMs, e.getMessage());
            throw new LLMProviderException("Gemini request failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Float> embed(String text) {
        // Embedding via Spring AI EmbeddingModel — injected separately in config
        // For MVP, return empty list; embedding implementation in Phase 5 (RAG)
        log.debug("Embedding request received — embedding model to be wired in Phase 5");
        return List.of();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isAvailable() {
        try {
            // Lightweight probe — will fail quickly if API key is invalid
            chatModel.call("ping");
            return true;
        } catch (Exception e) {
            log.warn("Gemini availability check failed: {}", e.getMessage());
            return false;
        }
    }

    // ─── Private Helpers ─────────────────────────────────────────────────────

    private List<org.springframework.ai.chat.messages.Message> buildMessages(LLMRequest request) {
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();

        // System prompt
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            messages.add(new SystemMessage(request.systemPrompt()));
        }

        // Historical messages
        if (request.messageHistory() != null) {
            for (LLMRequest.LLMMessage histMsg : request.messageHistory()) {
                messages.add(switch (histMsg.role().toLowerCase()) {
                    case "user" -> new UserMessage(histMsg.content());
                    case "assistant" -> new AssistantMessage(histMsg.content());
                    default -> new UserMessage(histMsg.content());
                });
            }
        }

        // Current user message
        if (request.userMessage() != null && !request.userMessage().isBlank()) {
            messages.add(new UserMessage(request.userMessage()));
        }

        return messages;
    }
}
