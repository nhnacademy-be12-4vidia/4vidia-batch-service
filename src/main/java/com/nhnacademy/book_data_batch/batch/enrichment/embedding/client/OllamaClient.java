package com.nhnacademy.book_data_batch.batch.enrichment.embedding.client;

import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class OllamaClient {

    private final RestClient restClient;
    private final String OLLAMA_URL = "http://ollama.java21.net/api/embeddings";

    public double[] generateEmbedding(String text) {

        try {
            Map<String, Object> request = Map.of(
                "model", "bge-m3",
                "prompt", text
            );

            EmbeddingResponse response = restClient
                    .post()
                    .uri(OLLAMA_URL)
                    .body(request)
                    .retrieve()
                    .body(EmbeddingResponse.class);

            return response != null ? response.getEmbedding() : new double[1024];
        } catch (Exception e) {
            log.error("Ollama 호출 에러: {}", e.getMessage());
            return null;
        }
    }

    @Data
    public static class EmbeddingResponse {

        private double[] embedding;
    }
}
