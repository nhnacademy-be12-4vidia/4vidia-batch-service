package com.nhnacademy.book_data_batch.jobs.book_import.config;

import com.nhnacademy.book_data_batch.domain.entity.Book;
import com.nhnacademy.book_data_batch.domain.entity.BookImage;
import com.nhnacademy.book_data_batch.domain.entity.Publisher;
import com.nhnacademy.book_data_batch.domain.repository.BatchRepository;
import com.nhnacademy.book_data_batch.domain.repository.BookImageRepository;
import com.nhnacademy.book_data_batch.domain.repository.BookRepository;
import com.nhnacademy.book_data_batch.domain.repository.PublisherRepository;
import com.nhnacademy.book_data_batch.global.util.FieldNormalizer;
import com.nhnacademy.book_data_batch.global.util.IsbnResolver;
import com.nhnacademy.book_data_batch.infrastructure.jdbc.JdbcExecutor;
import com.nhnacademy.book_data_batch.jobs.book_import.cache.InMemoryReferenceDataCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
@SpringBootTest(classes = {
    BookDataJobConfig.class,
    BookImportJobConfigTest.TestConfig.class,
    InMemoryReferenceDataCache.class,
    IsbnResolver.class,
    FieldNormalizer.class,
    JdbcExecutor.class
})
@EnableAutoConfiguration
@ActiveProfiles("test")
@TestPropertySource(properties = "batch.book.resource=classpath:data/book_test.csv")
class BookImportJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PublisherRepository publisherRepository;

    @Autowired
    private BookImageRepository bookImageRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private Job bookDataImportJob;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @TestConfiguration
    @EnableJpaRepositories(basePackages = "com.nhnacademy.book_data_batch.domain.repository")
    @EntityScan(basePackages = "com.nhnacademy.book_data_batch.domain")
    @ComponentScan(basePackages = "com.nhnacademy.book_data_batch.domain.repository.impl")
    @EnableTransactionManagement
    @EnableJpaAuditing
    static class TestConfig {
    }

    @AfterEach
    void tearDown() {
        bookImageRepository.deleteAll();
        bookRepository.deleteAll();
        publisherRepository.deleteAll();
        batchRepository.deleteAll();
    }

    @Test
    @DisplayName("도서 데이터 임포트 배치 테스트")
    void bookDataImportJob_importsBooksCorrectly() throws Exception {
        // Given
        // data/book_test.csv

        // When
        jobLauncherTestUtils.setJob(bookDataImportJob);
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        new TransactionTemplate(transactionManager).execute(status -> {
            // 1. Publisher 확인
            List<Publisher> publishers = publisherRepository.findAll();
            assertThat(publishers).hasSize(2);
            assertThat(publishers).extracting("name")
                    .containsExactlyInAnyOrder("Test Publisher", "Test Publisher 2");

            // 2. Book 확인
            List<Book> books = bookRepository.findAll();
            assertThat(books).hasSize(2);
            
            Book book1 = bookRepository.findAllByIsbnIn(List.of("9781234567890")).stream().findFirst().orElseThrow();
            assertThat(book1.getTitle()).isEqualTo("Test Book");
            assertThat(book1.getPublisher().getName()).isEqualTo("Test Publisher");
            assertThat(book1.getPriceStandard()).isEqualTo(10000);

            // 3. BookImage 확인
            List<BookImage> images = bookImageRepository.findAll();
            assertThat(images).hasSize(2);

            return null;
        });
    }
}
