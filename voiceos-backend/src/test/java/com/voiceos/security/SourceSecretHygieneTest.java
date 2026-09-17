package com.voiceos.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceSecretHygieneTest {

    @Test
    void configurationDoesNotEmbedFallbackSecrets() throws IOException {
        String yaml = Files.readString(Path.of("src/main/resources/application.yml"));
        assertTrue(yaml.contains("secret: ${JWT_SECRET:}"));
        assertTrue(yaml.contains("vapi-secret: ${VAPI_WEBHOOK_SECRET:}"));
        assertTrue(yaml.contains("api-key: ${VAPI_API_KEY:}"));
        assertTrue(yaml.contains("username: ${DATABASE_USERNAME:voiceos}"));
        assertTrue(yaml.contains("password: ${DATABASE_PASSWORD:}"));
        assertFalse(yaml.contains("default_dev_secret_replace_in_production_must_be_very_long"));
        assertFalse(yaml.contains("changeme"));
        assertFalse(yaml.contains("dev-secret"));
    }

    @Test
    void javaSourcesDoNotHardcodeVapiOrJwtSecrets() throws IOException {
        Path root = Path.of("src/main/java");
        try (Stream<Path> files = Files.walk(root)) {
            List<Path> javaFiles = files.filter(path -> path.toString().endsWith(".java")).toList();
            for (Path file : javaFiles) {
                String content = Files.readString(file);
                assertFalse(content.contains("VAPI_API_KEY="), file.toString());
                assertFalse(content.contains("VAPI_WEBHOOK_SECRET="), file.toString());
                assertFalse(content.contains("JWT_SECRET="), file.toString());
                assertFalse(content.contains("sk-"), file.toString());
            }
        }
    }
}
