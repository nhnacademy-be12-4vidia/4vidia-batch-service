package com.nhnacademy.book_data_batch.jobs.image_cleanup;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ContentImageCleanupJobConfig {

    private static final String JOB_NAME = "contentImageCleanupJob";

    private final JobRepository jobRepository;

    @Bean
    public Job contentImageCleanupJob(Step contentImageCleanupStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(contentImageCleanupStep)
                .build();
    }
}
