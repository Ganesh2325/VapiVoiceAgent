package com.voiceos.vapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Standard Data Transfer Objects for Vapi Webhook protocol.
 * Supports tool-calls, assistant-requests, status-updates, end-of-call-reports, and transcripts.
 */
public class VapiWebhookDTOs {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VapiWebhookPayload(
            VapiMessage message,
            VapiCall call,
            String timestamp
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VapiMessage(
            String type,
            @JsonProperty("call") VapiCall call,
            @JsonProperty("toolCalls") List<VapiToolCall> toolCalls,
            @JsonProperty("toolCallList") List<VapiToolCall> toolCallList,
            @JsonProperty("functionCall") VapiFunction functionCall,
            @JsonProperty("status") String status,
            @JsonProperty("endedReason") String endedReason,
            @JsonProperty("transcript") String transcript,
            @JsonProperty("role") String role,
            @JsonProperty("summary") String summary,
            @JsonProperty("recordingUrl") String recordingUrl,
            @JsonProperty("stereoRecordingUrl") String stereoRecordingUrl,
            @JsonProperty("durationSeconds") Double durationSeconds,
            @JsonProperty("cost") Double cost,
            @JsonProperty("analysis") Map<String, Object> analysis,
            @JsonProperty("artifact") Map<String, Object> artifact
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VapiCall(
            String id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("assistantId") String assistantId,
            @JsonProperty("customer") VapiCustomer customer,
            @JsonProperty("type") String type,
            @JsonProperty("status") String status,
            @JsonProperty("startedAt") String startedAt,
            @JsonProperty("endedAt") String endedAt
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VapiCustomer(
            String number,
            String name,
            String email
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VapiToolCall(
            String id,
            String type,
            VapiFunction function
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VapiFunction(
            String name,
            Map<String, Object> arguments
    ) {}

    public record VapiToolCallResponse(
            List<VapiToolResult> results
    ) {
        public static VapiToolCallResponse of(List<VapiToolResult> results) {
            return new VapiToolCallResponse(results);
        }
    }

    public record VapiToolResult(
            String name,
            @JsonProperty("toolCallId") String toolCallId,
            String result,
            String error
    ) {
        public static VapiToolResult success(String toolCallId, String name, String result) {
            return new VapiToolResult(name, toolCallId, result, null);
        }

        public static VapiToolResult error(String toolCallId, String name, String errorMessage) {
            return new VapiToolResult(name, toolCallId, null, errorMessage);
        }
    }

    public record VapiAssistantResponse(
            Map<String, Object> assistant
    ) {}
}
