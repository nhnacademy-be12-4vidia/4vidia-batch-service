package com.nhnacademy.book_data_batch.batch.jobs;

import com.nhnacademy.book_data_batch.batch.domain.aladin.client.AladinQuotaTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Nonnull;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AladinNewBookImportJobConfig {

    private static final String ALADIN_NEW_BOOK_IMPORT_JOB_NAME = "aladinNewBookImportJob";

    private final JobRepository jobRepository;
    private final AladinQuotaTracker aladinQuotaTracker;

    @Bean
    public Job aladinNewBookImportJob(
            @Qualifier("aladinFetchStep") Step aladinFetchStep,
            @Qualifier("aladinEnrichmentStep") Step aladinEnrichmentStep,
            @Qualifier("embeddingEnrichmentStep") Step embeddingEnrichmentStep,
            @Qualifier("cleanupStep") Step cleanupStep
    ) {
        return new JobBuilder(ALADIN_NEW_BOOK_IMPORT_JOB_NAME, jobRepository)
                .listener(new JobExecutionListener() {
                    @Override
                    public void beforeJob(@Nonnull JobExecution jobExecution) {
                        aladinQuotaTracker.reset();
                    }
                })
                .start(aladinFetchStep)
                .next(aladinEnrichmentStep)
                .next(embeddingEnrichmentStep)
                .next(cleanupStep)
                .build();
    }
}
