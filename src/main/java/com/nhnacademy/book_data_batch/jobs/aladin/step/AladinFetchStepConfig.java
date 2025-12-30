package com.nhnacademy.book_data_batch.jobs.aladin.step;

import com.nhnacademy.book_data_batch.jobs.aladin.dto.AladinFetchWrapper;
import com.nhnacademy.book_data_batch.jobs.aladin.dto.api.AladinItemDto;
import com.nhnacademy.book_data_batch.jobs.aladin.processor.AladinFetchProcessor;
import com.nhnacademy.book_data_batch.jobs.aladin.reader.AladinFetchReader;
import com.nhnacademy.book_data_batch.jobs.aladin.writer.AladinFetchWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class AladinFetchStepConfig {

    private static final String ALADIN_FETCH_STEP_NAME = "aladinFetchStep";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    // Components (Injected from AladinFetchReaderConfig and @Component)
    private final AladinFetchReader aladinFetchReader;
    private final AladinFetchProcessor aladinFetchProcessor;
    private final AladinFetchWriter aladinFetchWriter;

    // Fetch 전용 Chunk Size (API 50개 단위이므로 50 권장)
    private static final int FETCH_CHUNK_SIZE = 50;

    @Bean
    public Step aladinFetchStep() {
        return new StepBuilder(ALADIN_FETCH_STEP_NAME, jobRepository)
                .<AladinItemDto, AladinFetchWrapper>chunk(FETCH_CHUNK_SIZE, transactionManager)
                .reader(aladinFetchReader)
                .processor(aladinFetchProcessor)
                .writer(aladinFetchWriter)
                .build();
    }
}
