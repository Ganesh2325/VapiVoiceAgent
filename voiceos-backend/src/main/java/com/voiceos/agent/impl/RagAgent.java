package com.voiceos.agent.impl;

import com.voiceos.agent.core.Agent;
import com.voiceos.agent.core.AgentContext;
import com.voiceos.agent.core.AgentResult;
import com.voiceos.ai.LLMProvider;
import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.ToolRegistry;
import com.voiceos.tool.core.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * RAG Agent (Retrieval-Augmented Generation).
 * Interacts with DocumentSearchTool to query the Qdrant vector database 
 * and synthesize answers from user documents.
 */
@Component
public class RagAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(RagAgent.class);

    private final ToolRegistry toolRegistry;
    private final LLMProvider llmProvider;

    public RagAgent(ToolRegistry toolRegistry, LLMProvider llmProvider) {
        this.toolRegistry = toolRegistry;
        this.llmProvider = llmProvider;
    }

    @Override
    public String getName() {
        return "RagAgent";
    }

    @Override
    public String getDescription() {
        return "Retrieves information from uploaded documents, PDFs, and knowledge bases using vector search.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        String input = context.userInput().toLowerCase();
        return input.contains("document") || input.contains("file") || input.contains("resume") 
            || input.contains("specification") || input.contains("search knowledge") || input.contains("pdf");
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startMs = System.currentTimeMillis();
        String input = context.userInput();
        log.info("RagAgent executing for query: {}", input);

        // Retrieve relevant documents using DocumentSearchTool
        ToolResult searchResult = toolRegistry.executeTool("document_search", Map.of(
                "query", input,
                "userId", context.userId() != null ? context.userId().toString() : "anonymous"
        ));

        long latency = System.currentTimeMillis() - startMs;

        if (searchResult.success()) {
            // In a real implementation, you would pass the retrieved chunks to the LLM to synthesize an answer.
            // For now, we return the structured search result.
            String response = "I searched the knowledge base and found relevant documents.\n" +
                              "Here is a summary of what I found based on your request:\n" + searchResult.rawOutput();
            return AgentResult.withTools(getName(), response, List.of(searchResult), latency);
        } else {
            return AgentResult.withTools(getName(), "I'm sorry, I encountered an error while searching the document repository.", List.of(searchResult), latency);
        }
    }

    @Override
    public List<Tool> getTools() {
        return toolRegistry.getAllTools().stream()
                .filter(t -> t.getName().equals("document_search"))
                .toList();
    }
}
