package com.nhnacademy.book_data_batch.batch.enrichment;

import com.nhnacademy.book_data_batch.batch.enrichment.aladin.client.AladinApiClient;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.client.AladinQuotaTracker;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.mapper.AladinDataMapper;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.tasklet.AladinApiTasklet;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.tasklet.AladinSaveTasklet;
import com.nhnacademy.book_data_batch.batch.enrichment.context.EnrichmentResultsHolder;
import com.nhnacademy.book_data_batch.infrastructure.client.OllamaClient;
import com.nhnacademy.book_data_batch.batch.enrichment.embedding.tasklet.EmbeddingProcessTasklet;
import com.nhnacademy.book_data_batch.batch.enrichment.embedding.tasklet.EmbeddingIndexTasklet;
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
    private static final String ALADIN_API_STEP_NAME = "aladinApiStep";
    private static final String ALADIN_SAVE_STEP_NAME = "aladinSaveStep";
    private static final String EMBEDDING_PROCESS_STEP_NAME = "embeddingProcessStep";
    private static final String EMBEDDING_INDEX_STEP_NAME = "embeddingIndexStep";
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
    
    private final EnrichmentResultsHolder resultsHolder;

    @Bean
    public Job aladinEnrichmentJob(
            @Qualifier("aladinApiStep") Step aladinApiStep,
            @Qualifier("aladinSaveStep") Step aladinSaveStep,
            @Qualifier("embeddingProcessStep") Step embeddingProcessStep,
            @Qualifier("embeddingIndexStep") Step embeddingIndexStep,
            @Qualifier("cleanupStep") Step cleanupStep) {
        
        return new JobBuilder(ALADIN_JOB_NAME, jobRepository)
                .start(aladinApiStep)
                .next(aladinSaveStep)
                .next(embeddingProcessStep)
                .next(embeddingIndexStep)
                // 테스트용: Aladin API 호출 없이 Embedding만 실행 (querydsl, document도 같이 수정 필요)
//                .start(embeddingProcessStep)
                .next(cleanupStep)
                .build();
    }

    /**
     * Aladin API로 도서 정보 조회 (트랜잭션 없음 - I/O 작업만 수행)
     * Virtual Threads로 병렬 처리하면서 DB 연결 점유 안 함
     */
    @Bean
    public Step aladinApiStep() {
        return new StepBuilder(ALADIN_API_STEP_NAME, jobRepository)
                .tasklet(new AladinApiTasklet(
                        batchRepository,
                        aladinQuotaTracker,
                        aladinApiClient,
                        aladinDataMapper,
                        aladinApiKeys,
                        resultsHolder
                ), transactionManager)
                .build();
    }

    /**
     * API 결과를 DB에 저장 (독립 트랜잭션)
     * 이전 Step이 완료되고 DB 연결만 필요
     */
    @Bean
    public Step aladinSaveStep() {
        return new StepBuilder(ALADIN_SAVE_STEP_NAME, jobRepository)
                .tasklet(new AladinSaveTasklet(
                        authorRepository,
                        bookAuthorRepository,
                        tagRepository,
                        bookTagRepository,
                        bookRepository,
                        bookImageRepository,
                        batchRepository,
                        resultsHolder
                ), transactionManager)
                .build();
    }

    /**
     * Ollama 임베딩 생성 (트랜잭션 없음 - I/O 작업만 수행)
     * Virtual Threads로 병렬 처리하면서 DB 연결 점유 안 함
     */
    @Bean
    public Step embeddingProcessStep() {
        return new StepBuilder(EMBEDDING_PROCESS_STEP_NAME, jobRepository)
                .tasklet(new EmbeddingProcessTasklet(
                        batchRepository,
                        ollamaClient,
                        resultsHolder
                ), transactionManager)
                .build();
    }

    /**
     * Elasticsearch 인덱싱 및 상태 업데이트 (독립 트랜잭션)
     * 이전 Step이 완료되고 DB/ES 저장만 필요
     */
    @Bean
    public Step embeddingIndexStep() {
        return new StepBuilder(EMBEDDING_INDEX_STEP_NAME, jobRepository)
                .tasklet(new EmbeddingIndexTasklet(
                        bookSearchRepository,
                        batchRepository,
                        resultsHolder
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
