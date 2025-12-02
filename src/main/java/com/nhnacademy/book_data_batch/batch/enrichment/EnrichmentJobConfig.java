package com.nhnacademy.book_data_batch.batch.enrichment;

import com.nhnacademy.book_data_batch.batch.enrichment.client.AladinApiClient;
import com.nhnacademy.book_data_batch.batch.enrichment.dto.BookEnrichmentTarget;
import com.nhnacademy.book_data_batch.batch.enrichment.dto.EnrichmentResultDto;
import com.nhnacademy.book_data_batch.batch.enrichment.extractor.AladinAuthorExtractor;
import com.nhnacademy.book_data_batch.batch.enrichment.extractor.CategoryTagExtractor;
import com.nhnacademy.book_data_batch.batch.enrichment.handler.EnrichmentSaveHandler;
import com.nhnacademy.book_data_batch.batch.enrichment.listener.QuotaExhaustedStepListener;
import com.nhnacademy.book_data_batch.batch.enrichment.mapper.AladinBookMapper;
import com.nhnacademy.book_data_batch.batch.enrichment.partitioner.AladinEnrichmentPartitioner;
import com.nhnacademy.book_data_batch.batch.enrichment.processor.AladinEnrichmentProcessor;
import com.nhnacademy.book_data_batch.batch.enrichment.reader.AladinEnrichmentReader;
import com.nhnacademy.book_data_batch.batch.enrichment.writer.AladinEnrichmentWriter;
import com.nhnacademy.book_data_batch.repository.BatchRepository;
import com.nhnacademy.book_data_batch.repository.BookRepository;
import com.nhnacademy.book_data_batch.service.openapi.AladinQuotaService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * <pre>
 * EnrichmentJobConfig: Aladin API를 이용한 도서 정보 보강 배치 Job 설정
 * - 이름: enrichmentJob
 * - 목적: PENDING 상태의 도서를 Aladin API로 보강
 * - 방식: 8개 API 키로 병렬 파티셔닝 (하루 최대 4만건)
 * - 쿼터 제한: 파티션당 5,000건/일 (총 4만건/일)
 * 
 * [구조]
 * - Master Step: 파티션 분배
 * - Worker Step: API 호출 및 DB 저장
 * 
 * [쿼터 초과 처리]
 * - 파티션별 5,000건 초과 시 해당 파티션만 종료
 * - QUOTA_EXHAUSTED 상태로 정상 종료 처리
 * - 미처리 도서는 다음 날 Job 실행 시 처리
 * </pre>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class EnrichmentJobConfig {

    private static final String JOB_NAME = "enrichmentJob";
    private static final String MASTER_STEP_NAME = "aladinMasterStep";
    private static final String WORKER_STEP_NAME = "aladinWorkerStep";

    private static final int CHUNK_SIZE = 100;  // API 호출 단위
    private static final int GRID_SIZE = 8;     // 병렬 파티션 수 (= API 키 수)

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final AladinApiClient aladinApiClient;

    // Repository
    private final BatchRepository batchRepository;
    private final BookRepository bookRepository;
    private final EntityManager entityManager;
    
    // Mapper & Extractor
    private final AladinBookMapper bookMapper;
    private final AladinAuthorExtractor authorExtractor;
    private final CategoryTagExtractor tagExtractor;
    
    // Handler & Listener
    private final List<EnrichmentSaveHandler> saveHandlers;
    private final QuotaExhaustedStepListener quotaExhaustedStepListener;
    
    // Redis: api 사용량 관리
    private final AladinQuotaService quotaService;

    // Aladin API 키 목록
    @Value("${aladin.api.keys}")
    private List<String> aladinApiKeys;

    /**
     * Aladin Enrichment Job 정의
     */
    @Bean
    public Job enrichmentJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(aladinMasterStep())
                .build();
    }

    /**
     * Master Step - 파티션 분배
     */
    @Bean
    public Step aladinMasterStep() {
        return new StepBuilder(MASTER_STEP_NAME, jobRepository)
                .partitioner(WORKER_STEP_NAME, aladinPartitioner())
                .step(aladinWorkerStep())
                .gridSize(GRID_SIZE)
                .taskExecutor(aladinTaskExecutor())
                .build();
    }

    /**
     * Partitioner - PENDING 상태 도서를 파티션으로 분할 (Repository 기반)
     */
    @Bean
    public Partitioner aladinPartitioner() {
        return new AladinEnrichmentPartitioner(batchRepository, GRID_SIZE);
    }

    /**
     * Worker Step - 실제 API 호출 및 저장
     * - QuotaExhaustedStepListener: 쿼터 초과 시 QUOTA_EXHAUSTED 상태로 변환
     */
    @Bean
    public Step aladinWorkerStep() {
        return new StepBuilder(WORKER_STEP_NAME, jobRepository)
                .<BookEnrichmentTarget, EnrichmentResultDto>chunk(CHUNK_SIZE, transactionManager)
                .reader(aladinEnrichmentReader(null, null))
                .processor(aladinEnrichmentProcessor(null))
                .writer(aladinEnrichmentWriter())
                .listener(quotaExhaustedStepListener)
                .build();
    }

    /**
     * Reader - 파티션별 PENDING 도서 조회
     */
    @Bean
    @StepScope
    public RepositoryItemReader<BookEnrichmentTarget> aladinEnrichmentReader(
            @Value("#{stepExecutionContext['startId']}") Long startId,
            @Value("#{stepExecutionContext['endId']}") Long endId) {

        log.info("[Reader] 생성: startId={}, endId={}", startId, endId);
        return AladinEnrichmentReader.create(batchRepository, startId, endId);
    }

    /**
     * Processor - Book 조회 후 Aladin API 호출
     */
    @Bean
    @StepScope
    public ItemProcessor<BookEnrichmentTarget, EnrichmentResultDto> aladinEnrichmentProcessor(
            @Value("#{stepExecutionContext['partitionIndex']}") Integer partitionIndex) {

        // @StepScope 빈 초기화 시 null일 수 있음
        if (partitionIndex == null) {
            partitionIndex = 0;
        }
        
        String apiKey = aladinApiKeys.get(partitionIndex);
        log.info("[Processor] 생성: partitionIndex={}, apiKey={}***", partitionIndex, apiKey.substring(0, 5));
        
        return new AladinEnrichmentProcessor(
                bookRepository,
                entityManager,
                aladinApiClient, 
                bookMapper, 
                authorExtractor, 
                tagExtractor,
                quotaService,
                apiKey,
                partitionIndex
        );
    }

    /**
     * Writer - 보강 결과 저장 (Handler 기반)
     */
    @Bean
    public AladinEnrichmentWriter aladinEnrichmentWriter() {
        return new AladinEnrichmentWriter(saveHandlers);
    }

    /**
     * TaskExecutor - 병렬 실행용 스레드 풀
     */
    @Bean
    public TaskExecutor aladinTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(GRID_SIZE);
        executor.setMaxPoolSize(GRID_SIZE);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("aladin-enrichment-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
