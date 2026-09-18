package com.voiceos.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voiceos.api.dto.AuthDtos;
import com.voiceos.config.JwtProperties;
import com.voiceos.domain.repository.WebhookEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Test
    void validLoginSucceeds() throws Exception {
        String email = uniqueEmail("valid");
        register(email, "password12", "Valid User");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthDtos.LoginRequest(email, "password12"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    void invalidLoginRejected() throws Exception {
        String email = uniqueEmail("invalid");
        register(email, "password12", "Invalid User");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthDtos.LoginRequest(email, "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingCredentialsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingAuthenticationRejected() throws Exception {
        mockMvc.perform(get("/api/v1/conversations"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/voice/config"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/tools/calculator/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\":\"1+1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void voiceConfigDoesNotLeakSecrets() throws Exception {
        String token = register(uniqueEmail("voiceCfg"), "password12", "Voice Cfg").accessToken();
        MvcResult result = mockMvc.perform(get("/api/v1/voice/config")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.publicKeyPresent").value(true))
                .andExpect(jsonPath("$.assistantIdPresent").value(true))
                .andExpect(jsonPath("$.apiKey").doesNotExist())
                .andExpect(jsonPath("$.webhookSecret").doesNotExist())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains("sk-"));
        assertFalse(body.contains("\"apiKey\""));
        assertFalse(body.contains("\"webhookSecret\""));
        assertFalse(body.contains("\"api-key\""));
    }

    @Test
    void malformedTokenRejected() throws Exception {
        mockMvc.perform(get("/api/v1/conversations")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenRejected() throws Exception {
        String email = uniqueEmail("expired");
        AuthDtos.AuthResponse auth = register(email, "password12", "Expired User");
        JwtTokenProvider shortLived = new JwtTokenProvider(
                new JwtProperties(
                        "test_secret_key_that_is_at_least_64_characters_long_for_testing_purposes_only",
                        1,
                        1_000
                )
        );
        var principal = User.withUsername(email).password("x").authorities(List.of()).build();
        String expired = shortLived.generateAccessToken(principal, UUID.fromString(auth.user().id()));
        Thread.sleep(25);
        mockMvc.perform(get("/api/v1/conversations")
                        .header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanAccessOwnResource() throws Exception {
        String email = uniqueEmail("owner");
        String token = register(email, "password12", "Owner").accessToken();
        MvcResult created = mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Mine\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(get("/api/v1/conversations/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Mine"));
    }

    @Test
    void userCannotAccessAnotherUsersConversation() throws Exception {
        String tokenA = register(uniqueEmail("usera"), "password12", "User A").accessToken();
        String tokenB = register(uniqueEmail("userb"), "password12", "User B").accessToken();
        MvcResult created = mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Private A\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(get("/api/v1/conversations/" + id)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void authenticatedToolExecutionAllowed() throws Exception {
        String token = register(uniqueEmail("tools"), "password12", "Tools User").accessToken();
        mockMvc.perform(post("/api/v1/tools/calculator/execute")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\":\"2 + 2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void validVapiWebhookAccepted() throws Exception {
        mockMvc.perform(post("/api/webhooks/vapi")
                        .header("x-vapi-secret", "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusUpdatePayload("evt-valid-1", "call-valid-1")))
                .andExpect(status().isOk());
    }

    @Test
    void invalidVapiWebhookRejected() throws Exception {
        mockMvc.perform(post("/api/webhooks/vapi")
                        .header("x-vapi-secret", "wrong-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusUpdatePayload("evt-invalid-1", "call-invalid-1")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingVapiSecretRejected() throws Exception {
        mockMvc.perform(post("/api/webhooks/vapi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusUpdatePayload("evt-missing-1", "call-missing-1")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateWebhookDoesNotCreateSecondEvent() throws Exception {
        String token = register(uniqueEmail("duphook"), "password12", "Dup Hook").accessToken();
        bindSession(token, "call-dup-1");
        String body = calculatorPayload("stable-tool-call-dup", "call-dup-1");
        mockMvc.perform(post("/api/webhooks/vapi")
                        .header("x-vapi-secret", "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].result").value("4.00"));
        mockMvc.perform(post("/api/webhooks/vapi")
                        .header("x-vapi-secret", "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].result").value("4.00"));
        long matches = webhookEventRepository.findAll().stream()
                .filter(e -> "vapi:call:call-dup-1:tools:stable-tool-call-dup".equals(e.getIdempotencyKey()))
                .count();
        assertEquals(1, matches);
    }

    @Test
    void concurrentDuplicateWebhooksExecuteOnce() throws Exception {
        String token = register(uniqueEmail("conchook"), "password12", "Conc Hook").accessToken();
        bindSession(token, "call-conc-1");
        String body = calculatorPayload("stable-tool-call-conc", "call-conc-1");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        try {
            Future<MvcResult> first = executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return mockMvc.perform(post("/api/webhooks/vapi")
                                .header("x-vapi-secret", "test-secret")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                        .andReturn();
            });
            Future<MvcResult> second = executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return mockMvc.perform(post("/api/webhooks/vapi")
                                .header("x-vapi-secret", "test-secret")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                        .andReturn();
            });
            start.countDown();
            MvcResult resultA = first.get(15, TimeUnit.SECONDS);
            MvcResult resultB = second.get(15, TimeUnit.SECONDS);
            int statusA = resultA.getResponse().getStatus();
            int statusB = resultB.getResponse().getStatus();
            assertTrue(statusA == 200 || statusA == 409,
                    "statusA=" + statusA + " body=" + resultA.getResponse().getContentAsString());
            assertTrue(statusB == 200 || statusB == 409,
                    "statusB=" + statusB + " body=" + resultB.getResponse().getContentAsString());
            if (statusA == 200) {
                successes.incrementAndGet();
            }
            if (statusB == 200) {
                successes.incrementAndGet();
            }
            assertTrue(successes.get() >= 1);
        } finally {
            executor.shutdownNow();
        }
        long matches = webhookEventRepository.findAll().stream()
                .filter(e -> "vapi:call:call-conc-1:tools:stable-tool-call-conc".equals(e.getIdempotencyKey()))
                .count();
        assertEquals(1, matches);
    }

    private void bindSession(String token, String callId) throws Exception {
        mockMvc.perform(post("/api/v1/voice/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"callId\":\"" + callId + "\"}"))
                .andExpect(status().isCreated());
    }

    private AuthDtos.AuthResponse register(String email, String password, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthDtos.RegisterRequest(name, email, password))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthDtos.AuthResponse.class);
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "+" + UUID.randomUUID() + "@example.com";
    }

    private static String statusUpdatePayload(String eventId, String callId) {
        return """
                {"message":{"id":"%s","type":"status-update","status":"ended"},"call":{"id":"%s"},"timestamp":"2026-09-17T00:00:00Z"}
                """.formatted(eventId, callId);
    }

    private static String calculatorPayload(String toolCallId, String callId) {
        return objectSafe("""
                {
                  "message": {
                    "type": "tool-calls",
                    "toolCalls": [
                      {
                        "id": "%s",
                        "type": "function",
                        "function": { "name": "calculator", "arguments": { "expression": "2 + 2" } }
                      }
                    ]
                  },
                  "call": { "id": "%s" },
                  "timestamp": "2026-09-17T00:00:00Z"
                }
                """.formatted(toolCallId, callId));
    }

    private static String objectSafe(String json) {
        return json;
    }
}
