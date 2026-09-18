package com.voiceos.voice;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "voiceos.voice.config")
public class VoiceConfiguration {

    private String provider = "vapi";
    private String voiceId = "jennifer";
    private String language = "en-US";
    private String fallbackVoice = "alloy";
    private double speakingRate = 1.0;
    private String transcriber = "deepgram";
    private String transcriberModel = "nova-2";

    private String systemPrompt = "You are VoiceOS, a voice operations assistant. "
            + "For arithmetic, ALWAYS call the calculator tool with argument expression. "
            + "Never invent or guess the numeric result. "
            + "For any other user request, ALWAYS call voiceos_request with the user's utterance. "
            + "Do not answer general questions yourself. "
            + "Do not blindly guess names, emails, dates, or locations. "
            + "If a required value is missing, ask for it.";

    public Map<String, Object> toVapiAssistantConfig() {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("provider", "openai");
        model.put("model", "gpt-4o");
        model.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt)
        ));
        model.put("tools", List.of(calculatorTool(), voiceosRequestTool()));

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("name", "VoiceOS Calculator");
        config.put("firstMessage", "Hello! I am VoiceOS. Ask a question or ask me to calculate something.");
        config.put("model", model);
        config.put("voice", Map.of(
                "provider", "11labs",
                "voiceId", voiceId
        ));
        config.put("transcriber", Map.of(
                "provider", transcriber,
                "model", transcriberModel,
                "language", language,
                "smartFormat", true
        ));
        return config;
    }

    static Map<String, Object> calculatorTool() {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", Map.of(
                "expression", Map.of(
                        "type", "string",
                        "description", "Arithmetic expression such as 125 * 24"
                )
        ));
        parameters.put("required", List.of("expression"));

        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", "calculator");
        function.put("description", "Evaluate a mathematical expression. Always call this instead of computing the answer yourself.");
        function.put("parameters", parameters);

        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("type", "function");
        tool.put("function", function);
        return tool;
    }

    static Map<String, Object> voiceosRequestTool() {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", Map.of(
                "utterance", Map.of(
                        "type", "string",
                        "description", "The user's original spoken or typed request, unchanged"
                )
        ));
        parameters.put("required", List.of("utterance"));

        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", "voiceos_request");
        function.put("description", "Route any non-arithmetic user request to VoiceOS agents. Pass the original utterance. Do not invent the answer.");
        function.put("parameters", parameters);

        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("type", "function");
        tool.put("function", function);
        return tool;
    }

    // Getters and Setters
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getVoiceId() { return voiceId; }
    public void setVoiceId(String voiceId) { this.voiceId = voiceId; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getFallbackVoice() { return fallbackVoice; }
    public void setFallbackVoice(String fallbackVoice) { this.fallbackVoice = fallbackVoice; }

    public double getSpeakingRate() { return speakingRate; }
    public void setSpeakingRate(double speakingRate) { this.speakingRate = speakingRate; }

    public String getTranscriber() { return transcriber; }
    public void setTranscriber(String transcriber) { this.transcriber = transcriber; }

    public String getTranscriberModel() { return transcriberModel; }
    public void setTranscriberModel(String transcriberModel) { this.transcriberModel = transcriberModel; }
    
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
}
