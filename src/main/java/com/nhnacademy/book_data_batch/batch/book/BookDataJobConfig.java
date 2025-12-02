package com.nhnacademy.book_data_batch.batch.book;

import com.nhnacademy.book_data_batch.batch.book.cache.InMemoryReferenceDataCache;
import com.nhnacademy.book_data_batch.batch.book.processor.FieldNormalizer;
import com.nhnacademy.book_data_batch.batch.book.processor.IsbnResolver;
import com.nhnacademy.book_data_batch.batch.book.tasklet.BookImageTasklet;
import com.nhnacademy.book_data_batch.batch.book.tasklet.BookProcessingTasklet;
import com.nhnacademy.book_data_batch.batch.book.tasklet.ReferenceDataLoadTasklet;
import com.nhnacademy.book_data_batch.repository.BatchRepository;
import com.nhnacademy.book_data_batch.repository.BookImageRepository;
import com.nhnacademy.book_data_batch.repository.BookRepository;
import com.nhnacademy.book_data_batch.repository.CategoryRepository;
import com.nhnacademy.book_data_batch.repository.PublisherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * <pre>
 * BookDataJobConfig: 도서 CSV 데이터 등록 배치 Job 설정
 * 
 * [3단계 Tasklet 구조]
 * - Step 1: CSV 로드 + Publisher Bulk INSERT + Publisher/Category 캐시
 * - Step 2: Book 변환 + Book Bulk INSERT + Book 캐시
 * - Step 3: BookImage Bulk INSERT + Batch 기록 저장
 * </pre>
 */
@Configuration
@RequiredArgsConstructor
public class BookDataJobConfig {

    private static final String JOB_NAME = "bookDataImportJob";
    private static final String STEP1_NAME = "csvAndPublisherStep";
    private static final String STEP2_NAME = "bookProcessingStep";
    private static final String STEP3_NAME = "bookImageStep";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    // Repositories
    private final PublisherRepository publisherRepository;
    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;
    private final BookImageRepository bookImageRepository;
    private final BatchRepository batchRepository;

    // Components
    private final InMemoryReferenceDataCache cache;
    private final IsbnResolver isbnResolver;
    private final FieldNormalizer fieldNormalizer;

    /**
     * Book Data Import Job
     */
    @Bean
    public Job bookDataImportJob(
            Step csvAndPublisherStep,
            Step bookProcessingStep,
            Step bookImageStep) {
        
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(csvAndPublisherStep)
                .next(bookProcessingStep)
                .next(bookImageStep)
                .preventRestart()
                .build();
    }

    /**
     * Step 1: CSV 로드 + Publisher 처리
     */
    @Bean
    public Step csvAndPublisherStep(
            @Value("${batch.book.resource:classpath:data/BOOK_DB_202112.csv}") Resource csvResource) {
        
        return new StepBuilder(STEP1_NAME, jobRepository)
                .tasklet(new ReferenceDataLoadTasklet(
                        csvResource,
                        publisherRepository,
                        categoryRepository,
                        cache
                ), transactionManager)
                .build();
    }

    /**
     * Step 2: Book 처리
     */
    @Bean
    public Step bookProcessingStep() {
        return new StepBuilder(STEP2_NAME, jobRepository)
                .tasklet(new BookProcessingTasklet(
                        cache,
                        bookRepository,
                        isbnResolver,
                        fieldNormalizer
                ), transactionManager)
                .build();
    }

    /**
     * Step 3: BookImage + Batch 처리
     */
    @Bean
    public Step bookImageStep(
            @Value("${image.default.thumbnail}") String defaultThumbnailUrl) {
        return new StepBuilder(STEP3_NAME, jobRepository)
                .tasklet(new BookImageTasklet(
                        cache,
                        bookImageRepository,
                        batchRepository,
                        isbnResolver,
                        defaultThumbnailUrl
                ), transactionManager)
                .build();
    }
}
