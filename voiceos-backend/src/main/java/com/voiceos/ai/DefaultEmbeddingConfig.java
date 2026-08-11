package com.voiceos.ai;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Primary;

/**
 * Default / Mock EmbeddingModel configuration.
 * Ensures an {@link EmbeddingModel} bean is always present for Qdrant vector store auto-configuration,
 * even during unit tests or when running in offline mock mode.
 */
@Configuration
public class DefaultEmbeddingConfig {

    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "openAiEmbeddingModel")
    public EmbeddingModel defaultEmbeddingModel() {
        return new MockEmbeddingModel();
    }

    public static class MockEmbeddingModel implements EmbeddingModel {

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<Embedding> embeddings = new ArrayList<>();
            int index = 0;
            for (String text : request.getInstructions()) {
                embeddings.add(new Embedding(embed(text), index++));
            }
            return new EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(Document document) {
            String text = document.getText() != null ? document.getText() : "";
            return embed(text);
        }

        @Override
        public float[] embed(String text) {
            float[] vector = new float[384];
            int hash = text != null ? text.hashCode() : 0;
            for (int i = 0; i < 384; i++) {
                vector[i] = (float) Math.sin(hash + i);
            }
            return vector;
        }

        @Override
        public int dimensions() {
            return 384;
        }
    }
}
