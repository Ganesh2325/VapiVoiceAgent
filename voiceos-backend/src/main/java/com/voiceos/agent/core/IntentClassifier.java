package com.voiceos.agent.core;

/**
 * Extension point for structured intent labels. Does not select agents.
 * Specialized routing remains deterministic {@code canHandle} / tool ownership.
 */
public interface IntentClassifier {

    /**
     * @return intent label such as GENERAL_QUESTION, CALCULATION, TRAVEL, WRITING; never an agent name
     */
    String classify(AgentRequest request);
}
