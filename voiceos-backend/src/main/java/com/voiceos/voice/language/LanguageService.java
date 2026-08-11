package com.voiceos.voice.language;

import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class LanguageService {

    public enum Language {
        ENGLISH("en-US", "alloy"),
        HINDI("hi-IN", "shimmer"),
        TELUGU("te-IN", "shimmer"),
        TAMIL("ta-IN", "shimmer"),
        KANNADA("kn-IN", "shimmer"),
        MALAYALAM("ml-IN", "shimmer"),
        BENGALI("bn-IN", "shimmer"),
        MARATHI("mr-IN", "shimmer");

        private final String code;
        private final String defaultVoiceId;

        Language(String code, String defaultVoiceId) {
            this.code = code;
            this.defaultVoiceId = defaultVoiceId;
        }

        public String getCode() { return code; }
        public String getDefaultVoiceId() { return defaultVoiceId; }
        
        public static Language fromCode(String code) {
            for (Language lang : values()) {
                if (lang.code.equalsIgnoreCase(code)) return lang;
            }
            return ENGLISH;
        }
    }

    public Map<String, Object> getLanguageConfig(Language language) {
        return Map.of(
                "language", language.getCode(),
                "voiceId", language.getDefaultVoiceId()
        );
    }
}
