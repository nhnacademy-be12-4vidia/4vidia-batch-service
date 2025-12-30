package com.nhnacademy.book_data_batch.jobs.embedding.reader;

import com.nhnacademy.book_data_batch.domain.entity.Batch;
import com.nhnacademy.book_data_batch.domain.enums.BatchStatus;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class EmbeddingReaderConfig {

    private final EntityManagerFactory entityManagerFactory;

    @Value("${app.batch.chunk-size}")
    private int chunkSize;

    @Bean
    public JpaPagingItemReader<Batch> embeddingBatchReader() {
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("enrichmentStatus", BatchStatus.COMPLETED);

        return new JpaPagingItemReaderBuilder<Batch>()
                .name("embeddingBatchReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT b FROM Batch b " +
                        "JOIN FETCH b.book bk " +
                        "LEFT JOIN FETCH bk.publisher p " +
                        "LEFT JOIN FETCH bk.category c " +
                        "WHERE b.enrichmentStatus = :enrichmentStatus " +
                        "ORDER BY b.id ASC")
                .parameterValues(parameterValues)
                .pageSize(chunkSize)
                .saveState(false)  // 상태 저장 비활성화
                .build();
    }
}
