package com.nhnacademy.book_data_batch.repository.querydsl;

import com.nhnacademy.book_data_batch.batch.aladin.dto.BookEnrichmentTarget;
import com.nhnacademy.book_data_batch.entity.enums.BatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QuerydslBatchRepository {

    Long findMinIdByEnrichmentStatus(BatchStatus status);

    Long findMaxIdByEnrichmentStatus(BatchStatus status);

    Page<BookEnrichmentTarget> findPendingForEnrichment(
            BatchStatus status,
            Long startId,
            Long endId,
            Pageable pageable
    );
}
