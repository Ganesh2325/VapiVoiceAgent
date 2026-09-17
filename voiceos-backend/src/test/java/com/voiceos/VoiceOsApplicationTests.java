package com.voiceos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test — verifies the Spring application context loads without errors.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.ai.vertex.ai.gemini.project-id=voiceos-test",
        "spring.ai.vertex.ai.gemini.location=us-central1",
        "spring.ai.vertex.ai.gemini.chat.enabled=false",
        "spring.ai.openai.api-key=test-openai-key-for-unit-tests",
        "spring.ai.openai.chat.enabled=false",
        "spring.ai.openai.embedding.enabled=false",
        "voiceos.jwt.secret=test_secret_key_that_is_at_least_64_characters_long_for_testing_purposes_only",
        "voiceos.jwt.expiration-ms=86400000",
        "voiceos.jwt.refresh-expiration-ms=604800000",
        "voiceos.ai.provider=gemini",
        "voiceos.ai.mock=true",
        "voiceos.voice.provider=vapi",
        "voiceos.voice.vapi.api-key=test-vapi-key",
        "voiceos.voice.vapi.assistant-id=test-ast-id",
        "voiceos.voice.vapi.webhook-secret=test-secret",
        "voiceos.voice.vapi.public-key=test-pk",
        "voiceos.voice.vapi.base-url=https://api.vapi.ai",
        "voiceos.security.cors.allowed-origins=http://localhost:3000",
        "voiceos.security.rate-limit.enabled=false",
        "voiceos.security.rate-limit.requests-per-minute=60",
        "voiceos.agents.execution-timeout-ms=30000",
        "voiceos.agents.max-retries=2",
        "voiceos.rag.chunk-size=500",
        "voiceos.rag.chunk-overlap=50",
        "voiceos.rag.top-k=5",
        "voiceos.webhooks.vapi-secret=test-secret",
        "voiceos.webhooks.github-secret=",
        "voiceos.webhooks.calendar-secret=",
        "voiceos.webhooks.require-secret=true",
        "voiceos.webhooks.allow-insecure-local=false",
        "voiceos.integrations.github.token=",
        "voiceos.integrations.smtp.host=localhost",
        "voiceos.integrations.smtp.port=587",
        "voiceos.integrations.smtp.username=",
        "voiceos.integrations.smtp.password=",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreAutoConfiguration"
})
class VoiceOsApplicationTests {

    @Test
    void contextLoads() {
        // If this test passes, the Spring context started without errors.
        // This is the most important smoke test.
    }
}
