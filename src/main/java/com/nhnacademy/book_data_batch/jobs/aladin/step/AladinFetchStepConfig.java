package com.nhnacademy.book_data_batch.jobs.aladin.step;

import com.nhnacademy.book_data_batch.jobs.aladin.dto.AladinFetchWrapper;
import com.nhnacademy.book_data_batch.jobs.aladin.dto.api.AladinItemDto;
import com.nhnacademy.book_data_batch.jobs.aladin.processor.AladinFetchProcessor;
import com.nhnacademy.book_data_batch.jobs.aladin.reader.AladinFetchReader;
import com.nhnacademy.book_data_batch.jobs.aladin.writer.AladinFetchWriter;
import com.nhnacademy.book_data_batch.global.listener.SkipLoggingListener;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

@Configuration
@RequiredArgsConstructor
public class AladinFetchStepConfig {

    private static final String ALADIN_FETCH_STEP_NAME = "aladinFetchStep";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final AladinFetchReader aladinFetchReader;
    private final AladinFetchProcessor aladinFetchProcessor;
    private final AladinFetchWriter aladinFetchWriter;

    // Fetch 전용 Chunk Size (알라딘 리스트 조회 시 한번에 오는 데이터 개수)
    private static final int FETCH_CHUNK_SIZE = 50;

    @Bean
    public Step aladinFetchStep() {
        return new StepBuilder(ALADIN_FETCH_STEP_NAME, jobRepository)
                .<AladinItemDto, AladinFetchWrapper>chunk(FETCH_CHUNK_SIZE, transactionManager)
                .reader(aladinFetchReader)
                .processor(aladinFetchProcessor)
                .writer(aladinFetchWriter)
                .faultTolerant()
                .retryLimit(2)
                .retry(SocketTimeoutException.class)
                .retry(ConnectException.class)
                .skipLimit(5)
                .skip(IllegalArgumentException.class)
                .listener(new SkipLoggingListener())
                .build();
    }
}
