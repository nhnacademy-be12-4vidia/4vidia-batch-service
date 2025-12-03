package com.nhnacademy.book_data_batch.batch.enrichment.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class EmbeddingStepConfig {

    private static final String EMBEDDING_STEP_NAME = "embeddingStep";

    @Bean
    public Step embeddingStep() {
        return null;
    }
}
