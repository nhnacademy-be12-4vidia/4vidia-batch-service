package com.nhnacademy.book_data_batch.batch.domain.embedding.step;

import com.nhnacademy.book_data_batch.batch.domain.embedding.dto.EmbeddingEnrichmentResult;
import com.nhnacademy.book_data_batch.batch.domain.embedding.processor.EmbeddingItemProcessor;
import com.nhnacademy.book_data_batch.batch.domain.embedding.writer.EmbeddingItemWriter;
import com.nhnacademy.book_data_batch.domain.Batch;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class EmbeddingStepConfig {

    private static final String EMBEDDING_ENRICHMENT_STEP_NAME = "embeddingEnrichmentStep";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final EmbeddingItemProcessor embeddingItemProcessor;
    private final EmbeddingItemWriter embeddingItemWriter;

    @Value("${app.batch.chunk-size}")
    private int chunkSize;

    @Bean
    public Step embeddingEnrichmentStep(
            @Qualifier("embeddingBatchReader") JpaPagingItemReader<Batch> embeddingBatchReader
    ) {
        return new StepBuilder(EMBEDDING_ENRICHMENT_STEP_NAME, jobRepository)
                .<Batch, EmbeddingEnrichmentResult>chunk(chunkSize, transactionManager)
                .reader(embeddingBatchReader)
                .processor(embeddingItemProcessor)
                .writer(embeddingItemWriter)
                .listener(embeddingItemProcessor) // Register listener
                .build();
    }
}
