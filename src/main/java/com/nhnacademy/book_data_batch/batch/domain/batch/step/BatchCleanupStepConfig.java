package com.nhnacademy.book_data_batch.batch.domain.batch.step;

import com.nhnacademy.book_data_batch.infrastructure.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class BatchCleanupStepConfig {

    private static final String CLEANUP_STEP_NAME = "cleanupStep";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final BatchRepository batchRepository;

    @Bean
    public Step cleanupStep() {
        return new StepBuilder(CLEANUP_STEP_NAME, jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    batchRepository.deleteAllCompleted();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
