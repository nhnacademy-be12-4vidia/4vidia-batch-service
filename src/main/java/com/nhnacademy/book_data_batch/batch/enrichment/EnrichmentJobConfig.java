package com.nhnacademy.book_data_batch.batch.enrichment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enrichment Job 설정
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class EnrichmentJobConfig {

    private static final String JOB_NAME = "enrichmentJob";

    private final JobRepository jobRepository;

    @Bean
    public Job enrichmentJob(
            @Qualifier("loadPendingStep") Step loadPendingStep,
            @Qualifier("parallelApiCallStep") Step parallelApiCallStep,
            @Qualifier("bulkSaveStep") Step bulkSaveStep) {
        
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(loadPendingStep)
                .next(parallelApiCallStep)
                .next(bulkSaveStep)
                .build();
    }
}
