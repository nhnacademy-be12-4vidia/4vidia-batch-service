package com.nhnacademy.book_data_batch.jobs.aladin.config;

import com.nhnacademy.book_data_batch.domain.entity.Category;
import com.nhnacademy.book_data_batch.infrastructure.client.aladin.AladinApiClient;
import com.nhnacademy.book_data_batch.infrastructure.client.aladin.AladinQuotaTracker;
import com.nhnacademy.book_data_batch.jobs.aladin.dto.api.AladinItemDto;
import com.nhnacademy.book_data_batch.jobs.aladin.dto.api.AladinResponseDto;
import com.nhnacademy.book_data_batch.jobs.aladin.processor.AladinFetchProcessor;
import com.nhnacademy.book_data_batch.jobs.aladin.reader.AladinFetchReader;
import com.nhnacademy.book_data_batch.jobs.aladin.reader.AladinFetchReaderConfig;
import com.nhnacademy.book_data_batch.jobs.aladin.step.AladinFetchStepConfig;
import com.nhnacademy.book_data_batch.jobs.aladin.writer.AladinFetchWriter;
import com.nhnacademy.book_data_batch.infrastructure.jdbc.JdbcExecutor;
import com.nhnacademy.book_data_batch.global.util.IsbnResolver;
import com.nhnacademy.book_data_batch.global.util.FieldNormalizer;
import com.nhnacademy.book_data_batch.domain.repository.CategoryRepository;
import com.nhnacademy.book_data_batch.domain.repository.BookRepository;
import com.nhnacademy.book_data_batch.domain.repository.PublisherRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBatchTest
@SpringBootTest(classes = {
    AladinNewBookImportJobConfig.class,
    AladinFetchStepConfig.class,
    AladinFetchReaderConfig.class,
    AladinFetchProcessor.class,
    AladinFetchWriter.class,
    AladinNewBookImportJobConfigTest.TestConfig.class,
    JdbcExecutor.class,
    IsbnResolver.class,
    FieldNormalizer.class
}, properties = {
    "aladin.api.keys=test-key",
    "aladin.api.quota-per-key=100"
})
@EnableAutoConfiguration
@ActiveProfiles("test")
class AladinNewBookImportJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @MockitoBean
    private AladinApiClient aladinApiClient;

    @MockitoBean
    private AladinQuotaTracker aladinQuotaTracker;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PublisherRepository publisherRepository;

    @Autowired
    private Job aladinNewBookImportJob;

    @TestConfiguration
    @EnableJpaRepositories(basePackages = "com.nhnacademy.book_data_batch.domain.repository")
    @EntityScan(basePackages = "com.nhnacademy.book_data_batch.domain")
    @ComponentScan(basePackages = "com.nhnacademy.book_data_batch.domain.repository.impl")
    @EnableTransactionManagement
    @EnableJpaAuditing
    static class TestConfig {
        @Bean
        public Step aladinEnrichmentStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
            return mockStep("aladinEnrichmentStep", jobRepository, transactionManager);
        }

        @Bean
        public Step embeddingEnrichmentStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
            return mockStep("embeddingEnrichmentStep", jobRepository, transactionManager);
        }

        @Bean
        public Step cleanupStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
            return mockStep("cleanupStep", jobRepository, transactionManager);
        }

        private Step mockStep(String name, JobRepository jobRepository, PlatformTransactionManager transactionManager) {
            return new StepBuilder(name, jobRepository)
                    .tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, transactionManager)
                    .build();
        }
    }

    @AfterEach
    void tearDown() {
        bookRepository.deleteAll();
        categoryRepository.deleteAll();
        publisherRepository.deleteAll();
    }

    @Test
    @DisplayName("알라딘 신간 임포트 배치 테스트: API 호출 및 Fetch Step 완료 확인")
    void aladinNewBookImportJob_completesSuccessfully() throws Exception {
        // Given
        categoryRepository.save(Category.builder()
                .kdcCode("005")
                .categoryName("Computer")
                .path("/0/005")
                .depth(2)
                .build());

        AladinItemDto item = new AladinItemDto(
                "Test Title", "Author", "2023-01-01",
                "Description", "1234567890", "9781234567890", 
                10000, "http://cover", "Category", "Publisher", null
        );

        AladinResponseDto response = new AladinResponseDto(
                1, 1, 1, List.of(item), null, null
        );

        when(aladinQuotaTracker.isQuotaExhausted()).thenReturn(false);
        when(aladinQuotaTracker.tryAcquire(anyString())).thenReturn(true);
        when(aladinApiClient.listItems(anyInt(), anyString())).thenReturn(Optional.of(response));

        // When
        jobLauncherTestUtils.setJob(aladinNewBookImportJob);
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}