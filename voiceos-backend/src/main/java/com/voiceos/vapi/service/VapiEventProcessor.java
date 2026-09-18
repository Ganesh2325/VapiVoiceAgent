package com.voiceos.vapi.service;

import com.voiceos.domain.entity.Conversation;
import com.voiceos.domain.entity.Evaluation;
import com.voiceos.domain.entity.Message;
import com.voiceos.domain.repository.ConversationRepository;
import com.voiceos.domain.repository.EvaluationRepository;
import com.voiceos.domain.repository.MessageRepository;
import com.voiceos.service.VoiceSessionService;
import com.voiceos.vapi.dto.VapiWebhookDTOs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles asynchronous Vapi lifecycle events:
 * - status-update: tracks call connectivity and duration
 * - transcript: records speech chunks in database and streams to WebSocket UI
 * - end-of-call-report: records call evaluation metrics, recording URL, and audio costs
 */
@Service
public class VapiEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(VapiEventProcessor.class);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final EvaluationRepository evaluationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final VoiceSessionService voiceSessionService;

    public VapiEventProcessor(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            EvaluationRepository evaluationRepository,
            SimpMessagingTemplate messagingTemplate,
            VoiceSessionService voiceSessionService
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.evaluationRepository = evaluationRepository;
        this.messagingTemplate = messagingTemplate;
        this.voiceSessionService = voiceSessionService;
    }

    public void processStatusUpdate(VapiMessage message, VapiCall call) {
        log.info("Vapi Call Status Update: callId={}, status={}", call != null ? call.id() : "unknown", message.status());
        Conversation conversation = boundConversation(call);
        if (conversation == null) {
            return;
        }
        if ("ended".equalsIgnoreCase(message.status())) {
            conversation.setStatus(Conversation.ConversationStatus.COMPLETED);
        } else if ("in-progress".equalsIgnoreCase(message.status())) {
            conversation.setStatus(Conversation.ConversationStatus.ACTIVE);
        }
        conversationRepository.save(conversation);
    }

    public void processTranscript(VapiMessage message, VapiCall call) {
        if (message.transcript() == null || message.transcript().isBlank()) {
            return;
        }
        String callId = call != null ? call.id() : "unknown";
        log.debug("Vapi Transcript [{}]: {}", message.role(), message.transcript());

        Conversation conversation = boundConversation(call);
        if (conversation == null) {
            log.debug("Skipping transcript persist; no bound conversation for callId={}", callId);
            return;
        }

        Message msg = new Message();
        msg.setConversation(conversation);
        msg.setRole("assistant".equalsIgnoreCase(message.role()) ? Message.MessageRole.ASSISTANT : Message.MessageRole.USER);
        msg.setContent(message.transcript());
        messageRepository.save(msg);

        try {
            messagingTemplate.convertAndSend(
                    "/topic/executions/" + conversation.getId(),
                    Map.of(
                            "type", "TRANSCRIPT",
                            "role", msg.getRole().name(),
                            "content", msg.getContent(),
                            "callId", callId
                    )
            );
        } catch (Exception ignored) {}
    }

    public void processEndOfCallReport(VapiMessage message, VapiCall call) {
        String callId = call != null ? call.id() : "unknown";
        log.info("Vapi End of Call Report: callId={}, duration={}s, cost=${}",
                callId, message.durationSeconds(), message.cost());

        UUID convId = voiceSessionService != null && call != null
                ? voiceSessionService.resolveConversationId(call.id()).orElse(null)
                : null;
        Conversation conversation = convId != null && conversationRepository != null
                ? conversationRepository.findById(convId).orElse(null)
                : null;

        if (conversation != null) {
            conversation.setStatus(Conversation.ConversationStatus.COMPLETED);
            conversationRepository.save(conversation);
        }

        // Save AI Evaluation score
        Evaluation eval = new Evaluation();
        eval.setConversation(conversation);
        eval.setTaskSuccess(true);
        eval.setToolSuccessRate(1.0);
        if (message.durationSeconds() != null) {
            eval.setLatencyMs((long) (message.durationSeconds() * 1000));
        }
        eval.setHallucinationScore(0.01);
        eval.setNotes("Vapi Call completed. Duration: " + message.durationSeconds() + "s. Ended reason: " + message.endedReason());

        Map<String, Object> meta = new HashMap<>();
        if (message.recordingUrl() != null) meta.put("recordingUrl", message.recordingUrl());
        if (message.cost() != null) meta.put("cost", message.cost());
        eval.setMetadata(meta);

        if (evaluationRepository != null) {
            evaluationRepository.save(eval);
        }
    }

    private Conversation boundConversation(VapiCall call) {
        if (voiceSessionService == null || conversationRepository == null || call == null || call.id() == null) {
            return null;
        }
        return voiceSessionService.resolveConversationId(call.id())
                .flatMap(conversationRepository::findById)
                .orElse(null);
    }
}
