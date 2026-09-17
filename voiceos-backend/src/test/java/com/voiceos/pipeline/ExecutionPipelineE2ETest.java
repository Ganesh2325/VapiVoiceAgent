package com.voiceos.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voiceos.api.dto.AuthDtos;
import com.voiceos.domain.repository.WebhookEventRepository;
import com.voiceos.tool.impl.ForbiddenTool;
import com.voiceos.tool.impl.ProbeFailTool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

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
class ExecutionPipelineE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Autowired
    private ForbiddenTool forbiddenTool;

    @Autowired
    private ProbeFailTool probeFailTool;

    @Test
    void vapiCalculatorFlowCreatesCompletedAction() throws Exception {
        String token = register(uniqueEmail("calc"), "password12", "Calc User").accessToken();
        String callId = "call-calc-" + UUID.randomUUID();
        bindSession(token, callId);

        String toolCallId = "tool-calc-" + UUID.randomUUID();
        MvcResult result = mockMvc.perform(post("/api/webhooks/vapi")
                        .header("x-vapi-secret", "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(calculatorPayload(toolCallId, callId, "125 * 24")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].result").value("3000.00"))
                .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains("3000.00"));

        mockMvc.perform(get("/api/v1/actions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].agent").value("UtilityAgent"))
                .andExpect(jsonPath("$[0].tool").value("calculator"))
                .andExpect(jsonPath("$[0].provider").value("RealLocalCalculatorProvider"))
                .andExpect(jsonPath("$[0].providerMode").value("REAL"))
                .andExpect(jsonPath("$[0].result").value("3000.00"))
                .andExpect(jsonPath("$[0].verificationPassed").value(true));

        String actionId = objectMapper.readTree(
                mockMvc.perform(get("/api/v1/actions")
                                .header("Authorization", "Bearer " + token))
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        ).get(0).get("actionId").asText();

        mockMvc.perform(get("/api/v1/actions/" + actionId + "/timeline")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result").value("3000.00"))
                .andExpect(jsonPath("$.policyDecision").value("ALLOW"))
                .andExpect(jsonPath("$.verificationPassed").value(true))
                .andExpect(jsonPath("$.timeline[?(@.type=='ACTION_CREATED')]").isNotEmpty())
                .andExpect(jsonPath("$.timeline[?(@.type=='POLICY_EVALUATED')]").isNotEmpty())
                .andExpect(jsonPath("$.timeline[?(@.type=='TOOL_SUCCEEDED')]").isNotEmpty())
                .andExpect(jsonPath("$.timeline[?(@.type=='VERIFICATION_PASSED')]").isNotEmpty())
                .andExpect(jsonPath("$.timeline[?(@.type=='ACTION_COMPLETED')]").isNotEmpty())
                .andExpect(jsonPath("$.steps[0].sequenceNo").value(1))
                .andExpect(jsonPath("$.steps[?(@.stepType=='COMPLETION')]").isNotEmpty());
    }

    @Test
    void duplicateVapiEventDoesNotExecuteCalculatorTwice() throws Exception {
        String token = register(uniqueEmail("dup"), "password12", "Dup User").accessToken();
        String callId = "call-dup-e2e";
        bindSession(token, callId);
        String body = calculatorPayload("stable-e2e-dup", callId, "125 * 24");

        mockMvc.perform(post("/api/webhooks/vapi")
                        .header("x-vapi-secret", "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].result").value("3000.00"));
        mockMvc.perform(post("/api/webhooks/vapi")
                        .header("x-vapi-secret", "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].result").value("3000.00"));

        long matches = webhookEventRepository.findAll().stream()
                .filter(e -> "vapi:call:call-dup-e2e:tools:stable-e2e-dup".equals(e.getIdempotencyKey()))
                .count();
        assertEquals(1, matches);

        mockMvc.perform(get("/api/v1/actions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        String actionId = objectMapper.readTree(
                mockMvc.perform(get("/api/v1/actions")
                                .header("Authorization", "Bearer " + token))
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        ).get(0).get("actionId").asText();
        MvcResult timeline = mockMvc.perform(get("/api/v1/actions/" + actionId + "/events")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        long completed = objectMapper.readTree(timeline.getResponse().getContentAsString())
                .findValuesAsText("type").stream().filter("ACTION_COMPLETED"::equals).count();
        assertEquals(1, completed);
    }

    @Test
    void policyDenialDoesNotExecuteForbiddenTool() throws Exception {
        int before = forbiddenTool.executionCount();
        String token = register(uniqueEmail("deny"), "password12", "Deny User").accessToken();
        mockMvc.perform(post("/api/v1/tools/forbidden/execute")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value("REJECTED"));
        assertEquals(before, forbiddenTool.executionCount());
        String actionId = objectMapper.readTree(
                mockMvc.perform(get("/api/v1/actions")
                                .header("Authorization", "Bearer " + token))
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        ).get(0).get("actionId").asText();
        mockMvc.perform(get("/api/v1/actions/" + actionId + "/timeline")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.policyDecision").value("DENY"))
                .andExpect(jsonPath("$.timeline[?(@.type=='POLICY_DENIED')]").isNotEmpty())
                .andExpect(jsonPath("$.timeline[?(@.type=='ACTION_REJECTED')]").isNotEmpty())
                .andExpect(jsonPath("$.timeline[?(@.type=='TOOL_SUCCEEDED')]").isEmpty());
    }

    @Test
    void providerFailureMarksActionFailed() throws Exception {
        int before = probeFailTool.executionCount();
        String token = register(uniqueEmail("fail"), "password12", "Fail User").accessToken();
        mockMvc.perform(post("/api/v1/tools/probe_fail/execute")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value("FAILED"));
        assertEquals(before + 1, probeFailTool.executionCount());
        String actionId = objectMapper.readTree(
                mockMvc.perform(get("/api/v1/actions")
                                .header("Authorization", "Bearer " + token))
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        ).get(0).get("actionId").asText();
        mockMvc.perform(get("/api/v1/actions/" + actionId + "/timeline")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.verificationPassed").value(false))
                .andExpect(jsonPath("$.timeline[?(@.type=='PROVIDER_FAILED')]").isNotEmpty())
                .andExpect(jsonPath("$.timeline[?(@.type=='TOOL_FAILED')]").isNotEmpty())
                .andExpect(jsonPath("$.timeline[?(@.type=='VERIFICATION_PASSED')]").isEmpty())
                .andExpect(jsonPath("$.timeline[?(@.type=='ACTION_FAILED')]").isNotEmpty());
    }

    @Test
    void userCannotReadAnotherUsersAction() throws Exception {
        String tokenA = register(uniqueEmail("isoA"), "password12", "User A").accessToken();
        String tokenB = register(uniqueEmail("isoB"), "password12", "User B").accessToken();

        MvcResult created = mockMvc.perform(post("/api/v1/tools/calculator/execute")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\":\"3 + 4\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        String actionId = objectMapper.readTree(created.getResponse().getContentAsString()).get("actionId").asText();

        mockMvc.perform(get("/api/v1/actions/" + actionId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("7.00"));

        mockMvc.perform(get("/api/v1/actions/" + actionId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/actions/" + actionId + "/timeline")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/actions/" + actionId + "/steps")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/actions/" + actionId + "/events")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void flightRequestNeedsInformationAndDoesNotComplete() throws Exception {
        String token = register(uniqueEmail("flight"), "password12", "Flight User").accessToken();
        mockMvc.perform(post("/api/v1/agent/execute")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\":\"Book me a flight\",\"agent\":\"FinanceAgent\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_FOR_USER"))
                .andExpect(jsonPath("$.agent").value("TravelAgent"))
                .andExpect(jsonPath("$.success").value(false));

        String actionId = objectMapper.readTree(
                mockMvc.perform(get("/api/v1/actions")
                                .header("Authorization", "Bearer " + token))
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        ).get(0).get("actionId").asText();
        mockMvc.perform(get("/api/v1/actions/" + actionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_FOR_USER"))
                .andExpect(jsonPath("$.agent").value("TravelAgent"))
                .andExpect(jsonPath("$.agentOutcome").value("NEEDS_INFORMATION"))
                .andExpect(jsonPath("$.missingFields").isArray());
        mockMvc.perform(get("/api/v1/actions/" + actionId + "/timeline")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_FOR_USER"))
                .andExpect(jsonPath("$.timeline[?(@.type=='TOOL_SUCCEEDED')]").isEmpty())
                .andExpect(jsonPath("$.timeline[?(@.type=='ACTION_COMPLETED')]").isEmpty())
                .andExpect(jsonPath("$.timeline[?(@.type=='AGENT_SELECTED')]").isNotEmpty());
    }

    @Test
    void clientCannotSelectAgentOrProviderOnCalculator() throws Exception {
        String token = register(uniqueEmail("agentSel"), "password12", "Agent Sel").accessToken();
        mockMvc.perform(post("/api/v1/tools/calculator/execute")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\":\"125 * 24\",\"agent\":\"FinanceAgent\",\"agentName\":\"TravelAgent\",\"provider\":\"MockTravelProvider\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.rawOutput").value("3000.00"))
                .andExpect(jsonPath("$.agent").value("UtilityAgent"))
                .andExpect(jsonPath("$.provider").value("RealLocalCalculatorProvider"))
                .andExpect(jsonPath("$.providerMode").value("REAL"));
    }

    @Test
    void agentCatalogHasCapabilitiesAndNoConfidenceScores() throws Exception {
        String token = register(uniqueEmail("cat"), "password12", "Cat User").accessToken();
        MvcResult result = mockMvc.perform(get("/api/v1/agent/catalog")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='UtilityAgent')].capabilities").isNotEmpty())
                .andExpect(jsonPath("$[?(@.name=='UtilityAgent')].ownedTools").isNotEmpty())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains("0.92"));
        assertFalse(body.contains("96%"));
        assertFalse(body.contains("confidence"));
    }

    @Test
    void financeBudgetRequestDoesNotInventTotals() throws Exception {
        String token = register(uniqueEmail("fin"), "password12", "Fin User").accessToken();
        mockMvc.perform(post("/api/v1/agent/execute")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\":\"Show my expense breakdown\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agent").value("FinanceAgent"))
                .andExpect(jsonPath("$.status").value("WAITING_FOR_USER"))
                .andExpect(jsonPath("$.success").value(false));
        String body = mockMvc.perform(get("/api/v1/actions")
                        .header("Authorization", "Bearer " + token))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertFalse(body.contains("258"));
        assertFalse(body.contains("68.00"));
    }

    @Test
    void submittedProviderNameDoesNotChangeCalculatorProvider() throws Exception {
        String token = register(uniqueEmail("provSel"), "password12", "Prov User").accessToken();
        mockMvc.perform(post("/api/v1/tools/calculator/execute")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\":\"125 * 24\",\"provider\":\"MockTravelProvider\",\"providerClass\":\"com.voiceos.provider.travel.MockTravelProvider\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.rawOutput").value("3000.00"))
                .andExpect(jsonPath("$.provider").value("RealLocalCalculatorProvider"))
                .andExpect(jsonPath("$.providerMode").value("REAL"));
    }

    @Test
    void providerCatalogIsAuthenticatedAndContainsNoSecrets() throws Exception {
        mockMvc.perform(get("/api/v1/providers"))
                .andExpect(status().isUnauthorized());
        String token = register(uniqueEmail("provCat"), "password12", "Prov Cat").accessToken();
        MvcResult result = mockMvc.perform(get("/api/v1/providers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains("sk-"));
        assertFalse(body.toLowerCase().contains("bearer "));
        assertTrue(body.contains("RealLocalCalculatorProvider"));
        assertTrue(body.contains("MockTravelProvider"));
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

    private static String calculatorPayload(String toolCallId, String callId, String expression) {
        return """
                {
                  "message": {
                    "type": "tool-calls",
                    "toolCalls": [
                      {
                        "id": "%s",
                        "type": "function",
                        "function": { "name": "calculator", "arguments": { "expression": "%s" } }
                      }
                    ]
                  },
                  "call": { "id": "%s" },
                  "timestamp": "2026-09-17T00:00:00Z"
                }
                """.formatted(toolCallId, expression, callId);
    }
}
