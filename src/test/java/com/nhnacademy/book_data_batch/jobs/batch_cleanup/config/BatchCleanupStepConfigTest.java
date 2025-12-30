package com.nhnacademy.book_data_batch.jobs.batch_cleanup.config;

import com.nhnacademy.book_data_batch.domain.entity.Batch;
import com.nhnacademy.book_data_batch.domain.entity.Book;
import com.nhnacademy.book_data_batch.domain.enums.BatchStatus;
import com.nhnacademy.book_data_batch.domain.repository.BatchRepository;
import com.nhnacademy.book_data_batch.domain.repository.BookRepository;
import com.nhnacademy.book_data_batch.infrastructure.jdbc.JdbcExecutor;
import com.nhnacademy.book_data_batch.jobs.batch_cleanup.step.BatchCleanupStepConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
@SpringBootTest(classes = {
    BatchCleanupStepConfig.class,
    BatchCleanupStepConfigTest.TestConfig.class,
    JdbcExecutor.class
})
@EnableAutoConfiguration
@ActiveProfiles("test")
class BatchCleanupStepConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Job testCleanupJob;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @TestConfiguration
    @EnableJpaRepositories(basePackages = "com.nhnacademy.book_data_batch.domain.repository")
    @EntityScan(basePackages = "com.nhnacademy.book_data_batch.domain")
    @ComponentScan(basePackages = "com.nhnacademy.book_data_batch.domain.repository.impl")
    @EnableTransactionManagement
    @EnableJpaAuditing
    static class TestConfig {
        @Bean
        public Job testCleanupJob(JobRepository jobRepository, Step cleanupStep) {
            return new JobBuilder("testCleanupJob", jobRepository)
                    .start(cleanupStep)
                    .build();
        }
    }

    @AfterEach
    void tearDown() {
        batchRepository.deleteAll();
        bookRepository.deleteAll();
    }

    @Test
    @DisplayName("완료된 배치 레코드 삭제 테스트")
    void cleanupStep_deletesCompletedBatches() throws Exception {
        // Given
        Book book1 = Book.builder().title("Book 1").isbn("9781111111111").build();
        Book book2 = Book.builder().title("Book 2").isbn("9782222222222").build();
        bookRepository.saveAll(List.of(book1, book2));

        Batch batch1 = Batch.builder().book(book1).build();
        Batch batch2 = Batch.builder().book(book2).build();
        batchRepository.saveAll(List.of(batch1, batch2));

        // batch1: COMPLETED/COMPLETED (삭제 대상)
        jdbcTemplate.update("UPDATE batch SET enrichment_status = ?, embedding_status = ? WHERE book_id = ?",
                BatchStatus.COMPLETED.getCode(), BatchStatus.COMPLETED.getCode(), book1.getId());

        // batch2: COMPLETED/PENDING (삭제 대상 아님)
        jdbcTemplate.update("UPDATE batch SET enrichment_status = ?, embedding_status = ? WHERE book_id = ?",
                BatchStatus.COMPLETED.getCode(), BatchStatus.PENDING.getCode(), book2.getId());

        // When
        jobLauncherTestUtils.setJob(testCleanupJob);
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(org.springframework.batch.core.BatchStatus.COMPLETED);

        new TransactionTemplate(transactionManager).execute(status -> {
            List<Batch> remainingBatches = batchRepository.findAll();
            assertThat(remainingBatches).hasSize(1);
            assertThat(remainingBatches.get(0).getBook().getTitle()).isEqualTo("Book 2");
            return null;
        });
    }
}
