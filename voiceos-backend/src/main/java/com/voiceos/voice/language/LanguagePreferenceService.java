package com.voiceos.voice.language;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LanguagePreferenceService {

    // In a real implementation, this maps userId -> LanguagePreference entity in PostgreSQL
    private final Map<String, LanguagePreference> preferences = new ConcurrentHashMap<>();

    public LanguagePreference getPreference(String userId) {
        return preferences.getOrDefault(userId, new LanguagePreference(
                LanguageService.Language.ENGLISH, 
                true, // auto-detect true by default
                1.0   // normal speaking rate
        ));
    }

    public void setPreference(String userId, LanguagePreference preference) {
        preferences.put(userId, preference);
    }

    public record LanguagePreference(
            LanguageService.Language preferredLanguage,
            boolean autoDetectLanguage,
            double speechSpeed
    ) {}
}
