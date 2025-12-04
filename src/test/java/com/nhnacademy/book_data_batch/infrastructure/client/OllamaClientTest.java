package com.nhnacademy.book_data_batch.infrastructure.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OllamaClientTest {

    @Mock private RestClient restClient;
    @Mock private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private RestClient.RequestBodySpec requestBodySpec;
    @Mock private RestClient.ResponseSpec responseSpec;

    private OllamaClient ollamaClient;

    @BeforeEach
    void setUp() {
        ollamaClient = new OllamaClient(restClient, "http://localhost:11434/api/embeddings", "bge-m3");

        // RestClient 체이닝 설정
        given(restClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri(anyString())).willReturn(requestBodySpec);
        given(requestBodySpec.body(any(Map.class))).willReturn(requestBodySpec);
        given(requestBodySpec.retrieve()).willReturn(responseSpec);
    }

    @Test
    @DisplayName("정상 응답 시 임베딩 배열 반환")
    void generateEmbedding_success_returnsEmbedding() {
        // given
        double[] mockEmbedding = new double[]{0.1, 0.2, 0.3, 0.4, 0.5};
        OllamaClient.EmbeddingResponse response = new OllamaClient.EmbeddingResponse();
        response.setEmbedding(mockEmbedding);

        given(responseSpec.body(OllamaClient.EmbeddingResponse.class)).willReturn(response);

        // when
        double[] result = ollamaClient.generateEmbedding("테스트 텍스트");

        // then
        assertThat(result)
                .isNotNull()
                .hasSize(5);
        assertThat(result[0]).isEqualTo(0.1);
    }

    @Test
    @DisplayName("응답이 null이면 null 반환")
    void generateEmbedding_nullResponse_returnsNull() {
        // given
        given(responseSpec.body(OllamaClient.EmbeddingResponse.class)).willReturn(null);

        // when
        double[] result = ollamaClient.generateEmbedding("테스트 텍스트");

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("응답의 embedding이 null이면 null 반환")
    void generateEmbedding_nullEmbedding_returnsNull() {
        // given
        OllamaClient.EmbeddingResponse response = new OllamaClient.EmbeddingResponse();
        response.setEmbedding(null);

        given(responseSpec.body(OllamaClient.EmbeddingResponse.class)).willReturn(response);

        // when
        double[] result = ollamaClient.generateEmbedding("테스트 텍스트");

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("예외 발생 시 null 반환")
    void generateEmbedding_exception_returnsNull() {
        // given
        given(responseSpec.body(OllamaClient.EmbeddingResponse.class))
                .willThrow(new RestClientException("Connection refused"));

        // when
        double[] result = ollamaClient.generateEmbedding("테스트 텍스트");

        // then
        assertThat(result).isNull();
    }
}
