package com.nhnacademy.book_data_batch.batch.book;

import com.nhnacademy.book_data_batch.batch.book.dto.BookCsvRow;
import com.nhnacademy.book_data_batch.batch.book.dto.BookNormalizedItem;
import com.nhnacademy.book_data_batch.batch.book.processor.BookItemProcessor;
import com.nhnacademy.book_data_batch.batch.book.reader.BookCsvItemReader;
import com.nhnacademy.book_data_batch.batch.book.writer.BookItemWriter;
import com.nhnacademy.book_data_batch.repository.AuthorRepository;
import com.nhnacademy.book_data_batch.repository.BatchRepository;
import com.nhnacademy.book_data_batch.repository.BookAuthorRepository;
import com.nhnacademy.book_data_batch.repository.BookRepository;
import com.nhnacademy.book_data_batch.repository.CategoryRepository;
import com.nhnacademy.book_data_batch.repository.PublisherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * BOOK_DB_202112.csv → Book/Author/Publisher/Batch 엔티티로 저장하는 Spring Batch Job 설정입니다.
 */
@Configuration
@RequiredArgsConstructor
public class BookDataJobConfig {

    private static final String JOB_NAME = "bookDataImportJob";
    private static final String STEP_NAME = "bookDataImportStep";
    private static final int CHUNK_SIZE = 500;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;
    private final BookAuthorRepository bookAuthorRepository;
    private final BatchRepository batchRepository;

    @Bean
    public Job bookDataImportJob(Step bookDataImportStep) {
        // 카테고리 초기화 이후 한 번만 실행하면 됨
        return new JobBuilder(JOB_NAME, jobRepository)
            .start(bookDataImportStep)
            .preventRestart()
            .build();
    }

    // Step: BOOK_DB_202112.csv → Book/Author/Publisher/Batch 저장
    @Bean
    public Step bookDataImportStep(SynchronizedItemStreamReader<BookCsvRow> bookCsvItemReader,
                                   ItemProcessor<BookCsvRow, BookNormalizedItem> bookItemProcessor,
                                   BookItemWriter bookItemWriter) {
        // CSV → 정규화 → DB 저장까지 하나의 청크형 Step 으로 구성
        return new StepBuilder(STEP_NAME, jobRepository)
                // TODO: 완전 못보던 문법인데? 제네렉인건 알겠는데, 이게 앞에 오는건 처음보는데
                // -> 이건 스텝 빌더의 제네릭 타입을 지정하는 문법입니다. StepBuilder 가 어떤 타입의 아이템을 처리하는지 명시하는 거죠.
                //    이렇게 하면 컴파일 타임에 타입 체크가 가능해져서, 잘못된 타입 사용을 방지할 수 있습니다. 딸깍이형 고마워요!
            .<BookCsvRow, BookNormalizedItem>chunk(CHUNK_SIZE, transactionManager)
            .reader(bookCsvItemReader)
            .processor(bookItemProcessor)
            .writer(bookItemWriter)
            .build();
    }

    // Reader: BOOK_DB_202112.csv 파일을 한 줄씩 읽어 BookCsvRow로 매핑
    @Bean
    @StepScope
    public SynchronizedItemStreamReader<BookCsvRow> bookCsvItemReader(
        @Value("${batch.book.resource:classpath:data/BOOK_DB_202112.csv}") Resource resource) {

        FlatFileItemReader<BookCsvRow> delegate = new BookCsvItemReader(resource);
        SynchronizedItemStreamReader<BookCsvRow> reader = new SynchronizedItemStreamReader<>();
        reader.setDelegate(delegate);
        return reader;
    }

    // Processor: BookCsvRow → BookNormalizedItem 변환
    @Bean
    @StepScope
    public ItemProcessor<BookCsvRow, BookNormalizedItem> bookItemProcessor() {
        return new BookItemProcessor();
    }

    // Writer: 정규화된 BookNormalizedItem을 DB 에 저장
    @Bean
    public BookItemWriter bookItemWriter() {
        return new BookItemWriter(
            authorRepository,
            publisherRepository,
            bookRepository,
            bookAuthorRepository,
            categoryRepository,
            batchRepository
        );
    }
}
