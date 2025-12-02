package com.nhnacademy.book_data_batch.batch.book;

import com.nhnacademy.book_data_batch.batch.book.cache.ReferenceDataCache;
import com.nhnacademy.book_data_batch.batch.book.dto.BookCsvRow;
import com.nhnacademy.book_data_batch.batch.book.dto.BookNormalizedItem;
import com.nhnacademy.book_data_batch.batch.book.partitioner.RangePartitioner;
import com.nhnacademy.book_data_batch.batch.book.processor.BookItemProcessor;
import com.nhnacademy.book_data_batch.batch.book.processor.FieldNormalizer;
import com.nhnacademy.book_data_batch.batch.book.reader.BookCsvItemReader;
import com.nhnacademy.book_data_batch.batch.book.processor.IsbnResolver;
import com.nhnacademy.book_data_batch.batch.book.tasklet.ReferenceDataLoadTasklet;
import com.nhnacademy.book_data_batch.batch.book.writer.BookItemWriter;
import com.nhnacademy.book_data_batch.repository.PublisherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * <pre>
 * BookDataJobConfig: 도서 CSV 데이터 등록 배치 Job 설정
 * - 이름: bookDataImportJob
 * - 목적: 대용량 CSV 파일(약 15만 건)을 DB에 적재
 * - 방식: 2단계 처리 (출판사 사전 로딩 + 병렬 파티셔닝)
 * - 작가: 알라딘 API에서 처리 (이 Job에서는 rawAuthor만 저장)
 * 
 * [2단계 구조]
 * - Step 1: 단일 스레드로 Publisher 미리 등록
 * - Step 2: 파티션들은 캐시 조회만 하므로 락 경쟁 없음
 * </pre>
 */
@Configuration
@RequiredArgsConstructor
public class BookDataJobConfig {

    private static final String JOB_NAME = "bookDataImportJob";
    private static final String REFERENCE_DATA_STEP_NAME = "referenceDataLoadStep";
    private static final String MASTER_STEP_NAME = "masterStep";
    private static final String WORKER_STEP_NAME = "workerStep";

    private static final int CHUNK_SIZE = 2000;
    private static final int GRID_SIZE = 8;
    private static final int TOTAL_ROWS = 157119;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final IsbnResolver isbnResolver;
    private final FieldNormalizer fieldNormalizer;
    private final BookItemWriter bookItemWriter;
    private final PublisherRepository publisherRepository;
    private final ReferenceDataCache referenceDataCache;

    /**
     * Book Data Import Job 정의
     */
    @Bean
    public Job bookDataImportJob(Step referenceDataLoadStep, Step masterStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(referenceDataLoadStep)
                .next(masterStep)
                .preventRestart()
                .build();
    }

    /**
     * Reference-Data Load Step (출판사만)
     */
    @Bean
    public Step referenceDataLoadStep(
            @Value("${batch.book.resource:classpath:data/BOOK_DB_202112.csv}") Resource csvResource) {
        
        return new StepBuilder(REFERENCE_DATA_STEP_NAME, jobRepository)
                .tasklet(new ReferenceDataLoadTasklet(
                        csvResource,
                        publisherRepository,
                        referenceDataCache
                ), transactionManager)
                .build();
    }

    /**
     * Master Step
     */
    @Bean
    public Step masterStep(
            Partitioner partitioner,
            Step workerStep,
            TaskExecutor taskExecutor) {

        return new StepBuilder(MASTER_STEP_NAME, jobRepository)
                .partitioner(WORKER_STEP_NAME, partitioner)
                .step(workerStep)
                .gridSize(GRID_SIZE)
                .taskExecutor(taskExecutor)
                .build();
    }

    /**
     * Partitioner
     */
    @Bean
    public Partitioner partitioner() {
        return new RangePartitioner(TOTAL_ROWS);
    }

    /**
     * Worker Step
     */
    @Bean
    public Step workerStep(
            FlatFileItemReader<BookCsvRow> partitionedReader,
            ItemProcessor<BookCsvRow, BookNormalizedItem> bookItemProcessor) {

        return new StepBuilder(WORKER_STEP_NAME, jobRepository)
                .<BookCsvRow, BookNormalizedItem>chunk(CHUNK_SIZE, transactionManager)
                .reader(partitionedReader)
                .processor(bookItemProcessor)
                .writer(bookItemWriter)
                .build();
    }

    /**
     * 파티션별 CSV Reader
     */
    @Bean
    @StepScope
    public FlatFileItemReader<BookCsvRow> partitionedReader(
            @Value("#{stepExecutionContext['startRow']}") Integer startRow,
            @Value("#{stepExecutionContext['endRow']}") Integer endRow,
            @Value("${batch.book.resource:classpath:data/BOOK_DB_202112.csv}") Resource resource) {

        FlatFileItemReader<BookCsvRow> reader = new BookCsvItemReader(resource);
        reader.setLinesToSkip(startRow + 1);
        reader.setMaxItemCount(endRow - startRow);

        return reader;
    }

    /**
     * ItemProcessor (작가 파싱 제거됨)
     */
    @Bean
    public ItemProcessor<BookCsvRow, BookNormalizedItem> bookItemProcessor() {
        return new BookItemProcessor(isbnResolver, fieldNormalizer);
    }

    /**
     * TaskExecutor
     */
    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(GRID_SIZE);
        executor.setMaxPoolSize(GRID_SIZE * 2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("batch-partition-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
