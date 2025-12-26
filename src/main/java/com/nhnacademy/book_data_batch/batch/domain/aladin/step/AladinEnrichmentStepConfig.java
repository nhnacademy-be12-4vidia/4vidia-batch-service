package com.nhnacademy.book_data_batch.batch.domain.aladin.step;

import com.nhnacademy.book_data_batch.batch.core.dto.BookBatchTarget;
import com.nhnacademy.book_data_batch.batch.domain.aladin.dto.AladinEnrichmentResult;
import com.nhnacademy.book_data_batch.batch.domain.aladin.processor.AladinItemProcessor;
import com.nhnacademy.book_data_batch.batch.domain.aladin.writer.AladinItemWriter;
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
public class AladinEnrichmentStepConfig {

    private static final String ALADIN_ENRICHMENT_STEP_NAME = "aladinEnrichmentStep";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    // Aladin Components
    private final AladinItemProcessor aladinItemProcessor;
    private final AladinItemWriter aladinItemWriter;

    @Value("${app.batch.chunk-size}")
    private int chunkSize;

    @Bean
    public Step aladinEnrichmentStep(
            @Qualifier("aladinEnrichmentReader") JpaPagingItemReader<BookBatchTarget> aladinBatchReader
    ) {
        return new StepBuilder(ALADIN_ENRICHMENT_STEP_NAME, jobRepository)
                .<BookBatchTarget, AladinEnrichmentResult>chunk(chunkSize, transactionManager)
                .reader(aladinBatchReader)
                .processor(aladinItemProcessor)
                .writer(aladinItemWriter)
                .listener(aladinItemWriter)
                .faultTolerant()
                .skipLimit(1000)
                .skip(Exception.class)
                .build();
    }
}
