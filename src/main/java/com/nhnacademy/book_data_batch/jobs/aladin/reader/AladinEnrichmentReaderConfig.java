package com.nhnacademy.book_data_batch.jobs.aladin.reader;

import com.nhnacademy.book_data_batch.jobs.aladin.dto.BookBatchTarget;
import com.nhnacademy.book_data_batch.domain.enums.BatchStatus;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AladinEnrichmentReaderConfig {

    private final EntityManagerFactory entityManagerFactory;

    @Value("${aladin.api.keys}")
    private List<String> aladinApiKeys;

    @Value("${aladin.api.quota-per-key}")
    private int quotaPerKey;

    @Value("${app.batch.chunk-size}")
    private int chunkSize;

    @Bean
    public JpaPagingItemReader<BookBatchTarget> aladinEnrichmentReader() {
        log.info("[AladinBatchReader] PENDING 상태의 모든 배치 항목을 읽습니다.");

        return new JpaPagingItemReaderBuilder<BookBatchTarget>()
                .name("aladinEnrichmentReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString(
                        "SELECT new com.nhnacademy.book_data_batch.jobs.aladin.dto.BookBatchTarget(" +
                        "b.book.id, b.book.isbn, b.id) " +
                        "FROM Batch b JOIN b.book WHERE b.enrichmentStatus = :status " +
                        "ORDER BY b.id DESC")
                .parameterValues(Collections.singletonMap("status", BatchStatus.PENDING))
                .pageSize(chunkSize)
                .saveState(false)
                .build();
    }
}
