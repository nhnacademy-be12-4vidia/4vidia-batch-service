package com.nhnacademy.book_data_batch.batch.domain.aladin.reader;

import com.nhnacademy.book_data_batch.batch.core.dto.BookBatchTarget;
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
        // 전체 쿼터 계산 (키 개수 * 키당 쿼터)
        int totalQuota = aladinApiKeys.size() * quotaPerKey;
        log.info("[AladinBatchReader] 총 가용 쿼터: {}건 (키 {}개 * {})", totalQuota, aladinApiKeys.size(), quotaPerKey);

        return new JpaPagingItemReaderBuilder<BookBatchTarget>()
                .name("aladinEnrichmentReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT new com.nhnacademy.book_data_batch.batch.core.dto.BookBatchTarget(" +
                        "b.book.id, b.book.isbn, b.id) " +
                        "FROM Batch b JOIN b.book WHERE b.enrichmentStatus = :status " +
                        "ORDER BY b.id ASC")
                .parameterValues(Collections.singletonMap("status", BatchStatus.PENDING))
                .pageSize(chunkSize)
                .maxItemCount(totalQuota)
                .saveState(false)  // 상태 저장 비활성화
                .build();
    }
}
