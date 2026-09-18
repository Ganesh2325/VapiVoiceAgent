package com.voiceos.agent.impl;

import com.voiceos.agent.core.Agent;
import com.voiceos.agent.core.AgentCapability;
import com.voiceos.agent.core.AgentContext;
import com.voiceos.agent.core.AgentErrorCode;
import com.voiceos.agent.core.AgentOutcome;
import com.voiceos.agent.core.AgentRequest;
import com.voiceos.agent.core.AgentResult;
import com.voiceos.agent.core.KeywordIntentClassifier;
import com.voiceos.agent.decision.ModelDecision;
import com.voiceos.agent.decision.ModelDecisionParser;
import com.voiceos.agent.decision.ToolArgumentSanitizer;
import com.voiceos.ai.LLMProvider;
import com.voiceos.ai.LLMRequest;
import com.voiceos.ai.LLMRequest.LLMMessage;
import com.voiceos.ai.LLMResponse;
import com.voiceos.ai.gemini.LLMProviderException;
import com.voiceos.domain.entity.Message;
import com.voiceos.domain.repository.MessageRepository;
import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Universal fallback reasoning agent. Requests tools via structured output;
 * ActionEngine + PolicyEngine execute them. Never calls a Provider directly.
 */
@Component
public class GeneralQueryAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(GeneralQueryAgent.class);
    private static final int MAX_HISTORY = 8;
    static final Set<String> DEFAULT_ALLOWED_TOOLS = Set.of("calculator");
    private static final Pattern AMBIGUOUS = Pattern.compile(
            "(?i)^\\s*(what(?:'s| is) the best one|which one|that one|this one)\\??\\s*$");
    private static final Pattern CURRENT_WEATHER = Pattern.compile(
            "(?i)\\bweather\\b.*\\b(now|right now|currently|today|tonight)\\b|\\b(now|right now|currently|today|tonight)\\b.*\\bweather\\b");
    private static final Pattern CURRENT_MARKET = Pattern.compile(
            "(?i)\\b(stock|share price|bitcoin|exchange rate)\\b.*\\b(now|today|current|live)\\b|\\b(now|today|current|live)\\b.*\\b(stock|share price|bitcoin|exchange rate)\\b");
    private static final Pattern CURRENT_NEWS = Pattern.compile(
            "(?i)\\b(breaking news|news (today|right now|headline)|latest news)\\b");

    private static final String SYSTEM_PROMPT = """
            You are VoiceOS GeneralQueryAgent. Reply with a single JSON object only. No markdown.
            Schema:
            {"outcome":"ANSWER|CLARIFY|TOOL|UNAVAILABLE","intent":"GENERAL_QUESTION|EXPLANATION|WRITING|ANALYSIS|CALCULATION","answer":"string or null","toolCall":{"toolName":"calculator","arguments":{"expression":"..."}},"missingFields":[]}
            Rules:
            - Use TOOL with toolName calculator only for arithmetic that needs an exact numeric result.
            - Allowed tools: calculator. Never invent other tool names.
            - Do not include provider, providerClass, URL, credentials, agent names, SQL, or shell commands.
            - Do not invent citations, confidence scores, live weather, news, or prices.
            - Do not claim email, booking, payment, or calendar operations completed.
            - If the question is ambiguous, outcome=CLARIFY and list missingFields.
            - After a tool result is provided, answer using that actual result. If the tool failed, say so.
            """;

    private final LLMProvider llmProvider;
    private final MessageRepository messageRepository;
    private final KeywordIntentClassifier intentClassifier;
    private final Set<String> allowedTools;

    @Autowired
    public GeneralQueryAgent(LLMProvider llmProvider, @Autowired(required = false) MessageRepository messageRepository) {
        this(llmProvider, messageRepository, DEFAULT_ALLOWED_TOOLS);
    }

    public GeneralQueryAgent(LLMProvider llmProvider, MessageRepository messageRepository, Set<String> allowedTools) {
        this.llmProvider = llmProvider;
        this.messageRepository = messageRepository;
        this.intentClassifier = new KeywordIntentClassifier();
        this.allowedTools = allowedTools == null || allowedTools.isEmpty()
                ? DEFAULT_ALLOWED_TOOLS
                : allowedTools.stream().map(v -> v.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public String getName() {
        return "GeneralQueryAgent";
    }

    @Override
    public String getDescription() {
        return "Answers general questions via LLMProvider and may request server-approved low-risk tools.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return true;
    }

    @Override
    public int getPriority() {
        return 999;
    }

    @Override
    public Set<AgentCapability> capabilities() {
        return Set.of(AgentCapability.GENERAL_QUERY, AgentCapability.CONVERSATION);
    }

    @Override
    public boolean allowsTool(String toolName) {
        return toolName != null && allowedTools.contains(toolName.toLowerCase(Locale.ROOT));
    }

    @Override
    public AgentResult execute(AgentRequest request) {
        return reason(request, List.of());
    }

    @Override
    public AgentResult continueWith(AgentRequest request, List<ToolResult> toolResults) {
        return reason(request, toolResults != null ? toolResults : List.of());
    }

    @Override
    public AgentResult execute(AgentContext context) {
        return execute(AgentRequest.from(context, null));
    }

    @Override
    public List<Tool> getTools() {
        return List.of();
    }

    private AgentResult reason(AgentRequest request, List<ToolResult> toolResults) {
        long startMs = System.currentTimeMillis();
        String raw = request != null ? request.rawUserInput() : null;
        Map<String, Object> state = metadata(request, raw, null, null);
        if (request != null && request.requestedTool() != null && !request.requestedTool().isBlank()) {
            return AgentResult.unsupported(getName(),
                    "GeneralQueryAgent does not execute client-requested tool '" + request.requestedTool()
                            + "'. Tools must be selected from structured model output and run by ActionEngine.",
                    System.currentTimeMillis() - startMs);
        }

        if (raw == null || raw.isBlank()) {
            return AgentResult.needsInformation(getName(),
                    "I need the question or request you want me to answer.",
                    List.of("utterance"),
                    System.currentTimeMillis() - startMs);
        }

        List<LLMMessage> history = loadHistory(request);
        if (toolResults.isEmpty() && AMBIGUOUS.matcher(raw).matches() && history.isEmpty()) {
            return AgentResult.needsInformation(getName(),
                    "I need to know what you are referring to before I can answer.",
                    List.of("referent"),
                    System.currentTimeMillis() - startMs);
        }

        if (toolResults.isEmpty() && requiresUnavailableRealtime(raw)) {
            state.put("intent", "REALTIME");
            return AgentResult.unavailable(getName(),
                    "I cannot provide current live information for that request because no real-time data provider is configured. I will not invent the answer.",
                    state,
                    System.currentTimeMillis() - startMs);
        }

        if (llmProvider == null || !llmProvider.isAvailable()) {
            state.put("providerMode", "UNAVAILABLE");
            state.put("llmProvider", llmProvider != null ? llmProvider.getProviderName() : "unavailable");
            return AgentResult.unavailable(getName(),
                    "I'm unable to answer that right now because the reasoning service is unavailable.",
                    state,
                    System.currentTimeMillis() - startMs);
        }

        try {
            String userMessage = raw;
            if (!toolResults.isEmpty()) {
                userMessage = raw + "\n\n" + formatToolResults(toolResults);
            }
            LLMRequest llmRequest = LLMRequest.withHistory(SYSTEM_PROMPT, userMessage, history);
            LLMResponse response = llmProvider.chat(llmRequest);
            long latency = System.currentTimeMillis() - startMs;
            String content = response != null ? response.content() : null;
            if (content == null || content.isBlank()) {
                return new AgentResult(
                        getName(),
                        "I'm unable to answer that right now because the reasoning service returned no content.",
                        List.of(), false, null, null, false, metadata(request, raw, response, "FAILED"),
                        latency, AgentOutcome.FAILED, null, List.of(), AgentErrorCode.AGENT_EXECUTION_FAILED.name());
            }
            ModelDecision decision = ModelDecisionParser.parse(content);
            Map<String, Object> ok = metadata(request, raw, response, providerMode(response));
            if (decision.intent() != null && !decision.intent().isBlank()) {
                ok.put("intent", decision.intent());
            }
            if (decision.isClarify()) {
                List<String> missing = decision.missingFields().isEmpty() ? List.of("clarification") : decision.missingFields();
                return AgentResult.needsInformation(getName(),
                        decision.answer() != null ? decision.answer() : "I need more information before I can continue.",
                        missing, latency);
            }
            if (decision.isUnavailable()) {
                return AgentResult.unavailable(getName(),
                        decision.answer() != null ? decision.answer()
                                : "That capability is unavailable. I will not invent the answer.",
                        ok, latency);
            }
            if (decision.isTool()) {
                String toolName = decision.toolName();
                Map<String, Object> args = ToolArgumentSanitizer.sanitize(decision.arguments());
                ok.put("toolArguments", args);
                return AgentResult.requestTool(getName(), toolName, args, ok, latency);
            }
            String answer = decision.answer() != null && !decision.answer().isBlank() ? decision.answer() : content;
            return new AgentResult(
                    getName(), answer, List.of(), false, null, null, true, ok, latency,
                    AgentOutcome.COMPLETED, null, List.of(), null);
        } catch (LLMProviderException e) {
            log.warn("GeneralQueryAgent LLM failure: {}", e.getClass().getSimpleName());
            long latency = System.currentTimeMillis() - startMs;
            state.put("providerMode", "UNAVAILABLE");
            return new AgentResult(
                    getName(),
                    "I'm unable to answer that right now because the reasoning service is unavailable.",
                    List.of(), false, null, null, false, state, latency,
                    AgentOutcome.FAILED, null, List.of(), AgentErrorCode.AGENT_EXECUTION_FAILED.name());
        }
    }

    static boolean requiresUnavailableRealtime(String raw) {
        if (raw == null) {
            return false;
        }
        return CURRENT_WEATHER.matcher(raw).find()
                || CURRENT_MARKET.matcher(raw).find()
                || CURRENT_NEWS.matcher(raw).find();
    }

    static String inferIntent(String raw) {
        return new KeywordIntentClassifier().classify(new AgentRequest(null, null, null, null, null, raw, null, Map.of(), null));
    }

    private List<LLMMessage> loadHistory(AgentRequest request) {
        if (messageRepository == null || request == null || request.conversationId() == null) {
            return List.of();
        }
        UUID conversationId = request.conversationId();
        List<Message> rows = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        List<LLMMessage> history = new ArrayList<>();
        for (Message row : rows) {
            if (row == null || row.getContent() == null || row.getContent().isBlank() || row.getRole() == null) {
                continue;
            }
            String role = switch (row.getRole()) {
                case USER -> "user";
                case ASSISTANT -> "assistant";
                case SYSTEM -> "system";
                case TOOL -> null;
            };
            if (role == null) {
                continue;
            }
            if (row.getContent().equals(request.rawUserInput()) && "user".equals(role)) {
                continue;
            }
            history.add(new LLMMessage(role, row.getContent()));
        }
        if (history.size() > MAX_HISTORY) {
            return List.copyOf(history.subList(history.size() - MAX_HISTORY, history.size()));
        }
        return List.copyOf(history);
    }

    private static String formatToolResults(List<ToolResult> results) {
        StringBuilder sb = new StringBuilder();
        for (ToolResult result : results) {
            sb.append("Tool result (success=").append(result.success()).append("): ");
            if (result.success()) {
                sb.append(result.rawOutput() != null ? result.rawOutput() : String.valueOf(result.data()));
            } else {
                sb.append("FAILED: ").append(result.errorMessage());
                sb.append(". Do not invent a successful result.");
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private Map<String, Object> metadata(AgentRequest request, String raw, LLMResponse response, String mode) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("intent", request != null ? intentClassifier.classify(request) : inferIntent(raw));
        state.put("rawUserInput", raw);
        if (response != null) {
            state.put("llmProvider", response.providerName());
            state.put("model", response.model());
            if (response.inputTokens() != null) {
                state.put("inputTokens", response.inputTokens());
            }
            if (response.outputTokens() != null) {
                state.put("outputTokens", response.outputTokens());
            }
            state.put("providerMode", mode != null ? mode : providerMode(response));
        } else if (llmProvider != null) {
            state.put("llmProvider", llmProvider.getProviderName());
            state.put("providerMode", mode != null ? mode : ("mock".equalsIgnoreCase(llmProvider.getProviderName()) ? "MOCK" : "UNAVAILABLE"));
        }
        return state;
    }

    private static String providerMode(LLMResponse response) {
        if (response == null || response.providerName() == null) {
            return "UNAVAILABLE";
        }
        if ("mock".equalsIgnoreCase(response.providerName())) {
            return "MOCK";
        }
        if ("unavailable".equalsIgnoreCase(response.providerName())) {
            return "UNAVAILABLE";
        }
        return "REAL";
    }
}
