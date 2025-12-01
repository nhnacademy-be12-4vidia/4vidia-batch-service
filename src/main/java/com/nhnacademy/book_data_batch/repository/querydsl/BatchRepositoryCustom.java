package com.nhnacademy.book_data_batch.repository.querydsl;

import com.nhnacademy.book_data_batch.batch.aladin.dto.BookEnrichmentTarget;
import com.nhnacademy.book_data_batch.entity.enums.BatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Batch 엔티티의 복잡한 쿼리를 위한 Custom Repository
 */
public interface BatchRepositoryCustom {

    /**
     * 특정 상태의 batch_id 최솟값 조회
     */
    Long findMinIdByEnrichmentStatus(BatchStatus status);

    /**
     * 특정 상태의 batch_id 최댓값 조회
     */
    Long findMaxIdByEnrichmentStatus(BatchStatus status);

    /**
     * 파티션 범위 내 PENDING 상태의 보강 대상 도서 조회
     *
     * @param status   조회할 상태 (PENDING)
     * @param startId  시작 batch_id (inclusive)
     * @param endId    종료 batch_id (exclusive)
     * @param pageable 페이징 정보
     * @return 보강 대상 도서 정보 (DTO Projection)
     */
    Page<BookEnrichmentTarget> findPendingForEnrichment(
            BatchStatus status,
            Long startId,
            Long endId,
            Pageable pageable
    );
}
