package com.nhnacademy.book_data_batch.batch.enrichment.indexing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class IndexingStepConfig {

    private static final String INDEXING_STEP_NAME = "indexingStep";

    @Bean
    public Step indexingStep() {
        return null;
    }
}
