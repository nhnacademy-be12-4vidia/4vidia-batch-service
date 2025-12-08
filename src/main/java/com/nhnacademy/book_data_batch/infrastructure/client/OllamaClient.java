package com.nhnacademy.book_data_batch.infrastructure.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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
     * @return 임베딩 벡터 (실패 시 예외 발생 -> Tasklet에서 처리)
     */
    @Retryable(
            retryFor = {RestClientException.class, IllegalStateException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public double[] generateEmbedding(String text) {
        // try-catch 제거: 예외 발생 시 Retry가 동작하도록 함
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
            // null 응답도 재시도 대상에 포함시키기 위해 예외 발생
            throw new IllegalStateException("[OLLAMA] 응답이 비어있습니다.");
        }

        return response.getEmbedding();
    }

    @Data
    public static class EmbeddingResponse {
        private double[] embedding;
    }
}
