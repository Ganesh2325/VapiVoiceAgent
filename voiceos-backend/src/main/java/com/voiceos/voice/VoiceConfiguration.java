package com.voiceos.voice;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

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
    
    // Default system prompt
    private String systemPrompt = "You are VoiceOS, an AI operations assistant. " +
            "CRITICAL RULES: " +
            "1. You must not blindly guess names, emails, dates, or locations if you are unsure. " +
            "2. If an entity is ambiguous (e.g. 'Rahul' but there are multiple, or 'the 18th' but month is unclear), you MUST explicitly ask for clarification. " +
            "3. Before executing high-risk actions (like sending emails or payments), explicitly confirm the action with the user.";

    public Map<String, Object> toVapiAssistantConfig() {
        return Map.of(
                "name", "VoiceOS Orchestrator",
                "firstMessage", "Hello! I am VoiceOS. How can I help you today?",
                "model", Map.of(
                        "provider", "openai",
                        "model", "gpt-4o",
                        "messages", new Object[]{
                                Map.of(
                                        "role", "system",
                                        "content", systemPrompt
                                )
                        }
                ),
                "voice", Map.of(
                        "provider", "11labs", // Or configurable
                        "voiceId", voiceId
                ),
                "transcriber", Map.of(
                        "provider", transcriber,
                        "model", transcriberModel,
                        "language", language,
                        "smartFormat", true,
                        "keywords", new String[]{"VoiceOS", "Vapi", "Spring Boot", "React"}
                )
        );
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
