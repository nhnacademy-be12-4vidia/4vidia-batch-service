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
import com.nhnacademy.book_data_batch.repository.AuthorRepository;
import com.nhnacademy.book_data_batch.repository.PublisherRepository;
import com.nhnacademy.book_data_batch.service.AuthorNameExtractor;
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
 * - 방식: 2단계 처리 (참조 데이터 사전 로딩 + 병렬 파티셔닝)
 * 
 * [2단계 구조]
 * - Step 1: 단일 스레드로 모든 Author/Publisher 미리 등록
 * - Step 2: 파티션들은 캐시 조회만 하므로 락 경쟁 없음!
 * </pre>
 */
@Configuration
@RequiredArgsConstructor
public class BookDataJobConfig {

    private static final String JOB_NAME = "bookDataImportJob";
    private static final String REFERENCE_DATA_STEP_NAME = "referenceDataLoadStep";
    private static final String MASTER_STEP_NAME = "masterStep";
    private static final String WORKER_STEP_NAME = "workerStep";

    private static final int CHUNK_SIZE = 2000;   // 한 트랜잭션에서 처리할 레코드 수
    private static final int GRID_SIZE = 8;       // 병렬 처리할 파티션 수
    private static final int TOTAL_ROWS = 157119; // 주어진 도서 csv 파일의 행 수

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final IsbnResolver isbnResolver;
    private final AuthorNameExtractor authorNameExtractor;
    private final FieldNormalizer fieldNormalizer;
    private final BookItemWriter bookItemWriter;
    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final ReferenceDataCache referenceDataCache;

    /**
     * <pre>
     * Book Data Import Job 정의
     * - Step 1: 참조 데이터 사전 로딩
     * - Step 2: Master Step을 통한 병렬 처리
     * - 재시작 방지 설정 (중복 데이터 방지) TODO: 어... 이제 없어도 되지 않나? 일단 나중으로..
     *
     * @param referenceDataLoadStep 참조 데이터 로드 Step
     * @param masterStep 마스터 Step
     * @return 구성된 Job
     * </pre>
     */
    @Bean
    public Job bookDataImportJob(Step referenceDataLoadStep, Step masterStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(referenceDataLoadStep)  // Step 1: 참조 데이터 사전 로딩
                .next(masterStep)              // Step 2: Book 데이터 병렬 처리
                .preventRestart()              // 재시작 방지 (중복 데이터 방지)
                .build();
    }

    /**
     * <pre>
     * Reference-Data Load Step
     * - CSV 파일 -> 작가/출판사 이름 추출
     * - Bulk INSERT
     * - 전역 캐시 구축 (이후 Step에서 조회용)
     *
     * @param csvResource 도서 CSV 파일 리소스
     * @return 구성된 Step
     * </pre>
     */
    @Bean
    public Step referenceDataLoadStep(
            @Value("${batch.book.resource:classpath:data/BOOK_DB_202112.csv}") Resource csvResource) {
        
        return new StepBuilder(REFERENCE_DATA_STEP_NAME, jobRepository)
                .tasklet(new ReferenceDataLoadTasklet(
                        csvResource,
                        authorNameExtractor,
                        authorRepository,
                        publisherRepository,
                        referenceDataCache
                ), transactionManager)
                .build();
    }

    /**
     * <pre>
     * Master Step
     * - Partitioner로 CSV 파일을 GRID_SIZE개의 범위로 분할
     * - 각 범위를 Worker Step에 전달
     * - TaskExecutor로 병렬 실행
     * 
     * @param partitioner 파티션 분할 전략
     * @param workerStep 실제 처리를 담당할 Worker Step
     * @param taskExecutor 병렬 실행을 위한 스레드 풀
     * @return 구성된 Master Step
     * </pre>
     */
    @Bean
    public Step masterStep(
            Partitioner partitioner,
            Step workerStep,
            TaskExecutor taskExecutor) {

        return new StepBuilder(MASTER_STEP_NAME, jobRepository)
                .partitioner(WORKER_STEP_NAME, partitioner)  // 파티션 분배
                .step(workerStep)                            // 실행할 Worker Step
                .gridSize(GRID_SIZE)                         // 파티션 수
                .taskExecutor(taskExecutor)                  // 병렬 실행
                .build();
    }

    /**
     * <pre>
     * Partitioner
     * - 전체 CSV 행을 GRID_SIZE개의 범위로 분할
     * 
     * @return RangePartitioner 인스턴스
     * </pre>
     */
    @Bean
    public Partitioner partitioner() {
        return new RangePartitioner(TOTAL_ROWS);
    }

    /**
     * <pre>
     * Worker Step
     * - 할당받은 CSV 범위 처리
     * 
     * @param partitionedReader 파티션별 CSV Reader
     * @param bookItemProcessor 데이터 정규화 Processor
     * @return 구성된 Worker Step
     * </pre>
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
     * <pre>
     * 파티션별 CSV Reader 정의
     * - setLinesToSkip(startRow + 1): 헤더 + 이전 파티션 범위 스킵
     * - setMaxItemCount(endRow - startRow): 이 파티션에서 읽을 행 수
     * 
     * [StepScope]
     * - 각 파티션(스레드)마다 별도의 Reader 인스턴스 생성
     *   - 상태 공유 방지 (어디서부터 어디까지 읽을지가 step마다 다르다는 뜻)
     * - stepExecutionContext에서 파티션 범위 주입받음
     * 
     * @param startRow 시작 행 번호 (0-based)
     * @param endRow 종료 행 번호 (exclusive)
     * @param resource CSV 파일 리소스
     * @return 구성된 FlatFileItemReader
     * </pre>
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
     * <pre>
     * ItemProcessor 정의
     * - CSV 원시 데이터(BookCsvRow) → 정규화된 데이터(BookNormalizedItem)
     * - 유효성 검사 (ISBN이나 제목이 없는 경우 버림)
     * - 필드 변환 (날짜, 가격, KDC 코드 등)
     *
     * [StepScope] 불필요
     * - 상태를 가지지 않는 순수 유틸리티
     * 
     * @return 구성된 BookItemProcessor
     * </pre>
     */
    @Bean
    public ItemProcessor<BookCsvRow, BookNormalizedItem> bookItemProcessor() {
        return new BookItemProcessor(isbnResolver, authorNameExtractor, fieldNormalizer);
    }

    /**
     * <pre>
     * TaskExecutor 정의
     * - 병렬 파티셔닝을 위한 스레드 풀
     * 
     * [스레드 풀 설정]
     * - corePoolSize: 기본 스레드 수 (= GRID_SIZE = 파티션 수)
     * - maxPoolSize: 최대 스레드 수 (= GRID_SIZE * 2 = 파티션 수 * 2)
     *   - 버스트 상황 대응
     *   - 버스트 상황이란 일시적으로 많은 작업이 몰리는 경우
     *   - 우리 프로젝트에서는 파티션이 동시에 시작될 때 발생할 수 있음
     * - queueCapacity: 대기열 크기
     *   - maxPoolSize 초과 시 대기할 작업 수
     *   - 왜 필요한지: maxPoolSize를 초과하는 작업이 갑자기 몰릴 때
     *   - 대기열이 없으면 초과 작업이 즉시 거부됨
     * - RejectedExecutionHandler
     *   - CallerRunsPolicy: 대기열 초과 시 호출 스레드에서 직접 실행
     *   - 작업 유실 방지
     * </pre>
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
