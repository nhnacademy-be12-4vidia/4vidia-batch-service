package com.nhnacademy.book_data_batch.batch.enrichment.embedding.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Ollama 임베딩 API 클라이언트
 * - BGE-M3 모델 사용 (1024 dims)
 */
@Slf4j
@Component
public class OllamaClient {

    private final RestClient restClient;
    private final String ollamaUrl;
    private final String model;

    public OllamaClient(
            RestClient restClient,
            @Value("${ollama.api.url}") String ollamaUrl,
            @Value("${ollama.model:bge-m3}") String model
    ) {
        this.restClient = restClient;
        this.ollamaUrl = ollamaUrl;
        this.model = model;
    }

    /**
     * 텍스트에 대한 임베딩 벡터 생성
     *
     * @param text 임베딩할 텍스트
     * @return 임베딩 벡터 (실패 시 null)
     */
    public double[] generateEmbedding(String text) {
        try {
            Map<String, Object> request = Map.of(
                    "model", model,
                    "prompt", text
            );

            EmbeddingResponse response = restClient
                    .post()
                    .uri(ollamaUrl)
                    .body(request)
                    .retrieve()
                    .body(EmbeddingResponse.class);

            if (response == null || response.getEmbedding() == null) {
                log.warn("[OLLAMA] 응답 없음");
                return null;
            }

            return response.getEmbedding();
        } catch (Exception e) {
            log.error("[OLLAMA] 호출 실패: {}", e.getMessage());
            return null;
        }
    }

    @Data
    public static class EmbeddingResponse {
        private double[] embedding;
    }
}
