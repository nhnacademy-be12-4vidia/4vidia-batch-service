package com.nhnacademy.book_data_batch.jobs.embedding.config;

import com.nhnacademy.book_data_batch.domain.entity.Batch;
import com.nhnacademy.book_data_batch.domain.entity.Book;
import com.nhnacademy.book_data_batch.domain.enums.BatchStatus;
import com.nhnacademy.book_data_batch.domain.repository.BatchRepository;
import com.nhnacademy.book_data_batch.domain.repository.BookRepository;
import com.nhnacademy.book_data_batch.domain.repository.search.BookSearchRepository;
import com.nhnacademy.book_data_batch.infrastructure.client.ollama.OllamaClient;
import com.nhnacademy.book_data_batch.infrastructure.jdbc.JdbcExecutor;
import com.nhnacademy.book_data_batch.jobs.embedding.processor.EmbeddingItemProcessor;
import com.nhnacademy.book_data_batch.jobs.embedding.reader.EmbeddingReaderConfig;
import com.nhnacademy.book_data_batch.jobs.embedding.step.EmbeddingStepConfig;
import com.nhnacademy.book_data_batch.jobs.embedding.writer.EmbeddingItemWriter;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBatchTest
@SpringBootTest(classes = {
    EmbeddingStepConfig.class,
    EmbeddingReaderConfig.class,
    EmbeddingItemProcessor.class,
    EmbeddingItemWriter.class,
    EmbeddingStepConfigTest.TestConfig.class,
    JdbcExecutor.class
}, properties = {
    "app.batch.chunk-size=10"
})
@EnableAutoConfiguration
@ActiveProfiles("test")
class EmbeddingStepConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private OllamaClient ollamaClient;

    @MockitoBean
    private BookSearchRepository bookSearchRepository;

    @Autowired
    private Job testEmbeddingJob;

    @TestConfiguration
    @EnableJpaRepositories(basePackages = "com.nhnacademy.book_data_batch.domain.repository")
    @EntityScan(basePackages = "com.nhnacademy.book_data_batch.domain")
    @ComponentScan(basePackages = "com.nhnacademy.book_data_batch.domain.repository.impl")
    @EnableTransactionManagement
    @EnableJpaAuditing
    static class TestConfig {
        @Bean
        public Job testEmbeddingJob(JobRepository jobRepository, Step embeddingEnrichmentStep) {
            return new JobBuilder("testEmbeddingJob", jobRepository)
                    .start(embeddingEnrichmentStep)
                    .build();
        }
    }

    @AfterEach
    void tearDown() {
        batchRepository.deleteAll();
        bookRepository.deleteAll();
    }

    @Test
    @DisplayName("임베딩 생성 및 인덱싱 테스트")
    void embeddingStep_generatesEmbeddingsAndIndexes() throws Exception {
        // Given
        Book book = Book.builder()
                .title("Embedding Test Book")
                .isbn("9781111111111")
                .description("Test Description")
                .priceStandard(10000)
                .priceSales(9000)
                .build();
        bookRepository.save(book);

        // Batch 레코드 생성 (enrichmentStatus=COMPLETED, embeddingStatus=PENDING)
        // ID는 자동생성이므로 null, builder가 book만 받으므로 builder 사용 후 DB 직접 수정
        Batch batch = Batch.builder().book(book).build();
        batchRepository.save(batch);
        
        // 상태 강제 업데이트 (Builder/Setter 부재 대응)
        jdbcTemplate.update("UPDATE batch SET enrichment_status = ?, embedding_status = ? WHERE book_id = ?",
                BatchStatus.COMPLETED.getCode(), BatchStatus.PENDING.getCode(), book.getId());

        double[] mockEmbedding = new double[1024];
        for (int i = 0; i < 1024; i++) mockEmbedding[i] = i * 0.001;
        
        when(ollamaClient.generateEmbedding(anyString())).thenReturn(mockEmbedding);

        // When
        jobLauncherTestUtils.setJob(testEmbeddingJob);
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(org.springframework.batch.core.BatchStatus.COMPLETED);
        
        // 1. Ollama 호출 확인
        verify(ollamaClient).generateEmbedding(anyString());
        
        // 2. Elasticsearch 저장 확인
        verify(bookSearchRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
        
        // 3. DB 상태 확인
        List<Batch> remainingBatches = batchRepository.findAll();
        assertThat(remainingBatches).hasSize(1);
        assertThat(remainingBatches.getFirst().getEmbeddingStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}
