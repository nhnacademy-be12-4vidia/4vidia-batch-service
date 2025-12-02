package com.nhnacademy.book_data_batch.batch.enrichment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class EnrichmentJobConfig {

    private static final String JOB_NAME = "enrichmentJob";

    private final JobRepository jobRepository;
    private final Step aladinStep;

    @Bean
    public Job enrichmentJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(aladinStep)
                .build();
    }
}
