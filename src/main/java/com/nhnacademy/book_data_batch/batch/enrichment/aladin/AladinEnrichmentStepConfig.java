package com.nhnacademy.book_data_batch.batch.enrichment.aladin;

import com.nhnacademy.book_data_batch.batch.enrichment.aladin.client.AladinApiClient;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.mapper.AladinDataMapper;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.tasklet.AladinEnrichmentTasklet;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.tasklet.BulkSaveTasklet;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.tasklet.LoadPendingTasklet;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.tasklet.ParallelApiCallTasklet;
import com.nhnacademy.book_data_batch.batch.enrichment.common.EnrichmentCache;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.client.QuotaTracker;
import com.nhnacademy.book_data_batch.repository.AuthorRepository;
import com.nhnacademy.book_data_batch.repository.BatchRepository;
import com.nhnacademy.book_data_batch.repository.BookAuthorRepository;
import com.nhnacademy.book_data_batch.repository.BookImageRepository;
import com.nhnacademy.book_data_batch.repository.BookRepository;
import com.nhnacademy.book_data_batch.repository.BookTagRepository;
import com.nhnacademy.book_data_batch.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

/**
 * Aladin Enrichment Step 설정
 * 
 * <p>3-Step Tasklet 구조:</p>
 * <ol>
 *   <li>LoadPendingTasklet: PENDING 도서 전체 로드</li>
 *   <li>ParallelApiCallTasklet: 8개 스레드로 병렬 API 호출</li>
 *   <li>BulkSaveTasklet: 모든 결과 한 번에 저장</li>
 * </ol>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AladinEnrichmentStepConfig {

    private static final String STEP1_NAME = "loadPendingStep";
    private static final String STEP2_NAME = "parallelApiCallStep";
    private static final String STEP3_NAME = "bulkSaveStep";
    private static final String ALADIN_ENRICHMENT_STEP_NAME = "aladinEnrichmentStep";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    // Common
    private final EnrichmentCache enrichmentCache;
    private final QuotaTracker quotaTracker;

    // Aladin
    private final AladinApiClient aladinApiClient;
    private final AladinDataMapper aladinDataMapper;

    // Repositories
    private final BatchRepository batchRepository;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final BookAuthorRepository bookAuthorRepository;
    private final TagRepository tagRepository;
    private final BookTagRepository bookTagRepository;
    private final BookImageRepository bookImageRepository;

    @Value("${aladin.api.keys}")
    private List<String> aladinApiKeys;

    @Bean
    public Step aladinEnrichmentStep() {
        return new StepBuilder(ALADIN_ENRICHMENT_STEP_NAME, jobRepository)
                .tasklet(new AladinEnrichmentTasklet(
                        batchRepository,
                        authorRepository,
                        bookAuthorRepository,
                        tagRepository,
                        bookTagRepository,
                        bookRepository,
                        bookImageRepository,
                        quotaTracker,
                        aladinApiClient,
                        aladinDataMapper,
                        aladinApiKeys
                ), transactionManager)
                .build();
    }

    /**
     * Step 1: PENDING 도서 로드
     */
    @Bean
    public Step loadPendingStep() {
        return new StepBuilder(STEP1_NAME, jobRepository)
                .tasklet(new LoadPendingTasklet(
                        batchRepository,
                        enrichmentCache,
                        quotaTracker
                ), transactionManager)
                .build();
    }

    /**
     * Step 2: 병렬 API 호출
     */
    @Bean
    public Step parallelApiCallStep() {
        return new StepBuilder(STEP2_NAME, jobRepository)
                .tasklet(new ParallelApiCallTasklet(
                        enrichmentCache,
                        aladinApiClient,
                        aladinDataMapper,
                        quotaTracker,
                        aladinApiKeys
                ), transactionManager)
                .build();
    }

    /**
     * Step 3: Bulk 저장
     */
    @Bean
    public Step bulkSaveStep() {
        return new StepBuilder(STEP3_NAME, jobRepository)
                .tasklet(new BulkSaveTasklet(
                        enrichmentCache,
                        authorRepository,
                        bookAuthorRepository,
                        tagRepository,
                        bookTagRepository,
                        bookRepository,
                        bookImageRepository,
                        batchRepository
                ), transactionManager)
                .build();
    }
}
