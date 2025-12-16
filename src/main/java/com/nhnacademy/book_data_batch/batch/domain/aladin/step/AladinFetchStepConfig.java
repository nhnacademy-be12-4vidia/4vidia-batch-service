package com.nhnacademy.book_data_batch.batch.domain.aladin.step;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class AladinFetchStepConfig {

    private static final String ALADIN_FETCH_STEP_NAME = "aladinFetchStep";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Step aladinFetchStep(
            //@Qualifier("aladinFetchReader")
    ) {
        return null;
    }
}
