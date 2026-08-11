package com.voiceos.voice.language;

import org.springframework.stereotype.Service;

@Service
public class LanguageDetectionService {

    /**
     * Determines the most likely language from a transcript segment.
     * In a production scenario, this could use an external NLP API or rely on
     * Vapi's built-in language detection webhook events if available.
     * For now, this is a basic heuristic fallback.
     */
    public LanguageService.Language detectLanguage(String text) {
        if (text == null || text.isBlank()) return LanguageService.Language.ENGLISH;
        
        // Basic heuristic based on unicode blocks
        if (text.codePoints().anyMatch(c -> c >= 0x0900 && c <= 0x097F)) {
            // Devanagari script (Hindi, Marathi)
            // Simplified fallback
            return LanguageService.Language.HINDI;
        } else if (text.codePoints().anyMatch(c -> c >= 0x0C00 && c <= 0x0C7F)) {
            // Telugu script
            return LanguageService.Language.TELUGU;
        } else if (text.codePoints().anyMatch(c -> c >= 0x0B80 && c <= 0x0BFF)) {
            // Tamil script
            return LanguageService.Language.TAMIL;
        } else if (text.codePoints().anyMatch(c -> c >= 0x0C80 && c <= 0x0CFF)) {
            // Kannada script
            return LanguageService.Language.KANNADA;
        } else if (text.codePoints().anyMatch(c -> c >= 0x0D00 && c <= 0x0D7F)) {
            // Malayalam script
            return LanguageService.Language.MALAYALAM;
        } else if (text.codePoints().anyMatch(c -> c >= 0x0980 && c <= 0x09FF)) {
            // Bengali script
            return LanguageService.Language.BENGALI;
        }
        
        return LanguageService.Language.ENGLISH;
    }
}
