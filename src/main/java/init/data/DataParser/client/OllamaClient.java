package init.data.DataParser.client;

import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class OllamaClient {

    private final RestTemplate restTemplate;
    private final String OLLAMA_URL = "http://ollama.java21.net/api/enbeddings";

    public double[] generateEmbedding(String text) {

        try {
            Map<String, Object> request = Map.of(
                "model", "bge-m3",
                "prompt", text
            );

            EmbeddingResponse response = restTemplate.postForObject(OLLAMA_URL, request,
                EmbeddingResponse.class);
            return response != null ? response.getEmbedding() : new double[1024];
        } catch (Exception e) {
            return new double[1024];
        }
    }

    @Data
    public static class EmbeddingResponse {

        private double[] embedding;
    }


}
