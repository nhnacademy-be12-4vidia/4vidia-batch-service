package com.nhnacademy.book_data_batch.batch.jobs;

import com.nhnacademy.book_data_batch.batch.core.dto.BookBatchTarget;
import com.nhnacademy.book_data_batch.batch.domain.embedding.processor.EmbeddingItemProcessor;
import com.nhnacademy.book_data_batch.batch.domain.embedding.writer.EmbeddingItemWriter;
import com.nhnacademy.book_data_batch.batch.domain.embedding.dto.BookEmbeddingTarget;
import com.nhnacademy.book_data_batch.batch.domain.embedding.dto.EmbeddingEnrichmentResult;
import com.nhnacademy.book_data_batch.batch.domain.aladin.processor.AladinItemProcessor;
import com.nhnacademy.book_data_batch.batch.domain.aladin.writer.AladinItemWriter;
import com.nhnacademy.book_data_batch.batch.domain.aladin.client.AladinQuotaTracker;
import com.nhnacademy.book_data_batch.batch.domain.aladin.dto.AladinEnrichmentResult;
import com.nhnacademy.book_data_batch.domain.Batch;
import com.nhnacademy.book_data_batch.domain.enums.BatchStatus;
import com.nhnacademy.book_data_batch.infrastructure.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import jakarta.persistence.EntityManagerFactory;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class EnrichmentJobConfig {

    private static final String ALADIN_JOB_NAME = "aladinEnrichmentJob";
    private static final String ALADIN_ENRICHMENT_STEP_NAME = "aladinEnrichmentStep";
    private static final String EMBEDDING_ENRICHMENT_STEP_NAME = "embeddingEnrichmentStep";
    private static final String CLEANUP_STEP_NAME = "cleanupStep";
    private static final int CHUNK_SIZE = 80;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    // Aladin Components
    private final AladinItemProcessor aladinItemProcessor;
    private final AladinItemWriter aladinItemWriter;
    private final AladinQuotaTracker aladinQuotaTracker;

    // Embedding Components
    private final EmbeddingItemProcessor embeddingItemProcessor;
    private final EmbeddingItemWriter embeddingItemWriter;
    
    private final BatchRepository batchRepository;

    @Value("${aladin.api.keys}")
    private List<String> aladinApiKeys;

    @Value("${aladin.api.quota-per-key}")
    private int quotaPerKey;

    @Bean
    public Job aladinEnrichmentJob(
            @Qualifier("aladinEnrichmentStep") Step aladinEnrichmentStep,
            @Qualifier("embeddingEnrichmentStep") Step embeddingEnrichmentStep,
            @Qualifier("cleanupStep") Step cleanupStep) {
        
        Flow aladinFlow = new FlowBuilder<SimpleFlow>("aladinFlow")
                .start(aladinEnrichmentStep)
                .build();

        return new JobBuilder(ALADIN_JOB_NAME, jobRepository)
                .listener(new JobExecutionListener() {
                    @Override
                    public void beforeJob(@Nonnull JobExecution jobExecution) {
                        aladinQuotaTracker.reset();
                    }
                })
                .start(aladinFlow)
                    .on("QUOTA_EXHAUSTED").to(embeddingEnrichmentStep) // 쿼터 소진 시 임베딩 스탭으로
                    .from(aladinFlow).on("*").to(embeddingEnrichmentStep) // 나머지 모든 경우에도 임베딩 스탭으로
                .from(embeddingEnrichmentStep)
                    .on("*").to(cleanupStep)
                .end()
                .build();
    }

    @Bean
    public Step aladinEnrichmentStep(
            @Qualifier("aladinBatchReader") JpaPagingItemReader<BookBatchTarget> aladinBatchReader) {
        return new StepBuilder(ALADIN_ENRICHMENT_STEP_NAME, jobRepository)
                .<BookBatchTarget, AladinEnrichmentResult>chunk(CHUNK_SIZE, transactionManager)
                .reader(aladinBatchReader)
                .processor(aladinItemProcessor)
                .writer(aladinItemWriter)
                .listener(aladinItemWriter)
                .faultTolerant()
                .skipLimit(1000)
                .skip(Exception.class)
                .build();
    }

    @Bean(name = "aladinBatchReader")
    public JpaPagingItemReader<BookBatchTarget> aladinBatchReader() {
        // 전체 쿼터 계산 (키 개수 * 키당 쿼터)
        int totalQuota = aladinApiKeys.size() * quotaPerKey;
        log.info("[AladinBatchReader] 총 가용 쿼터: {}건 (키 {}개 * {})", totalQuota, aladinApiKeys.size(), quotaPerKey);

        return new JpaPagingItemReaderBuilder<BookBatchTarget>()
                .name("aladinBatchReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT new com.nhnacademy.book_data_batch.batch.core.dto.BookBatchTarget(" +
                        "b.book.id, b.book.isbn, b.id) " +
                        "FROM Batch b JOIN b.book WHERE b.enrichmentStatus = :status " +
                        "ORDER BY b.id ASC")
                .parameterValues(Collections.singletonMap("status", BatchStatus.PENDING))
                .pageSize(CHUNK_SIZE)
                .maxItemCount(totalQuota)
                .saveState(false)  // 상태 저장 비활성화
                .build();
    }

    @Bean
    public Step embeddingEnrichmentStep(
            @Qualifier("embeddingBatchReader") JpaPagingItemReader<Batch> embeddingBatchReader) {
        return new StepBuilder(EMBEDDING_ENRICHMENT_STEP_NAME, jobRepository)
                .<Batch, EmbeddingEnrichmentResult>chunk(CHUNK_SIZE, transactionManager)
                .reader(embeddingBatchReader)
                .processor(embeddingItemProcessor)
                .writer(embeddingItemWriter)
                .listener(embeddingItemProcessor) // Register listener
                .build();
    }

    @Bean(name = "embeddingBatchReader")
    public JpaPagingItemReader<Batch> embeddingBatchReader() {
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("enrichmentStatus", BatchStatus.COMPLETED);
        
        return new JpaPagingItemReaderBuilder<Batch>()
                .name("embeddingBatchReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT b FROM Batch b " +
                        "JOIN FETCH b.book bk " +
                        "LEFT JOIN FETCH bk.publisher p " +
                        "WHERE b.enrichmentStatus = :enrichmentStatus " +
                        "ORDER BY b.id ASC")
                .parameterValues(parameterValues)
                .pageSize(CHUNK_SIZE)
                .saveState(false)  // 상태 저장 비활성화
                .build();
    }



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