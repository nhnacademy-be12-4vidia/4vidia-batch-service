package com.nhnacademy.book_data_batch.repository.querydsl;

import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.BookEnrichmentTarget;
import com.nhnacademy.book_data_batch.entity.enums.BatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface QuerydslBatchRepository {

    Long findMinIdByEnrichmentStatus(BatchStatus status);

    Long findMaxIdByEnrichmentStatus(BatchStatus status);

    Page<BookEnrichmentTarget> findPendingForEnrichment(
            BatchStatus status,
            Long startId,
            Long endId,
            Pageable pageable
    );

    /**
     * PENDING 상태의 모든 도서 조회 (Tasklet용)
     * 
     * @return 보강 대상 도서 목록
     */
    List<BookEnrichmentTarget> findAllPending();
}
