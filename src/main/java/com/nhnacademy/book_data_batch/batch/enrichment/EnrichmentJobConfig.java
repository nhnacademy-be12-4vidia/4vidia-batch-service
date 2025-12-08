package com.nhnacademy.book_data_batch.batch.enrichment;

import com.nhnacademy.book_data_batch.batch.enrichment.aladin.client.AladinApiClient;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.client.AladinQuotaTracker;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.mapper.AladinDataMapper;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.tasklet.AladinEnrichmentTasklet;
import com.nhnacademy.book_data_batch.infrastructure.client.OllamaClient;
import com.nhnacademy.book_data_batch.batch.enrichment.embedding.tasklet.EmbeddingTasklet;
import com.nhnacademy.book_data_batch.infrastructure.repository.*;
import com.nhnacademy.book_data_batch.infrastructure.repository.search.BookSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

/**
 * Enrichment Job 설정
 * 
 * <p>Job 구성:</p>
 * <ol>
 *   <li>aladinEnrichmentStep - Aladin API로 도서 정보 보강</li>
 *   <li>embeddingStep - Ollama로 임베딩 생성 + Elasticsearch 인덱싱</li>
 *   <li>cleanupStep - 완료된 Batch 레코드 삭제</li>
 * </ol>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class EnrichmentJobConfig {

    private static final String ALADIN_JOB_NAME = "aladinEnrichmentJob";
    private static final String Nl_JOB_NAME = "nlEnrichmentJob";
    private static final String ALADIN_ENRICHMENT_STEP_NAME = "aladinEnrichmentStep";
    private static final String EMBEDDING_STEP_NAME = "embeddingStep";
    private static final String CLEANUP_STEP_NAME = "cleanupStep";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    // Aladin
    private final AladinApiClient aladinApiClient;
    private final AladinDataMapper aladinDataMapper;
    private final AladinQuotaTracker aladinQuotaTracker;

    // Embedding
    private final OllamaClient ollamaClient;
    private final BookSearchRepository bookSearchRepository;

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
    public Job enrichmentJob(
            @Qualifier("aladinEnrichmentStep") Step aladinEnrichmentStep,
            @Qualifier("embeddingStep") Step embeddingStep,
            @Qualifier("cleanupStep") Step cleanupStep) {
        
        return new JobBuilder(ALADIN_JOB_NAME, jobRepository)
                .start(aladinEnrichmentStep)
                .next(embeddingStep)
                // 테스트용: Aladin API 호출 없이 Embedding만 실행 (querydsl, document도 같이 수정 필요)
//                .start(embeddingStep)
                .next(cleanupStep)
                .build();
    }

    /**
     * Aladin API로 도서 정보 보강
     */
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
                        aladinQuotaTracker,
                        aladinApiClient,
                        aladinDataMapper,
                        aladinApiKeys
                ), transactionManager)
                .build();
    }

    /**
     * Ollama 임베딩 생성 + Elasticsearch 인덱싱
     */
    @Bean
    public Step embeddingStep() {
        return new StepBuilder(EMBEDDING_STEP_NAME, jobRepository)
                .tasklet(new EmbeddingTasklet(
                        batchRepository,
                        bookSearchRepository,
                        ollamaClient
                ), transactionManager)
                .build();
    }

    /**
     * 완료된 Batch 레코드 삭제
     */
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
