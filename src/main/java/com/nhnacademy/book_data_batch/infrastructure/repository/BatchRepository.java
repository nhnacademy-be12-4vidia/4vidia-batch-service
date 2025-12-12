package com.nhnacademy.book_data_batch.infrastructure.repository;

import com.nhnacademy.book_data_batch.batch.core.dto.BookBatchTarget;
import com.nhnacademy.book_data_batch.batch.domain.embedding.dto.BookEmbeddingTarget;
import com.nhnacademy.book_data_batch.domain.Batch;
import com.nhnacademy.book_data_batch.domain.enums.BatchStatus;
import com.nhnacademy.book_data_batch.infrastructure.repository.bulk.BulkBatchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BatchRepository extends JpaRepository<Batch, Long>, BulkBatchRepository {

    /**
     * 보강이 필요한 Batch 대상을 Page 형태로 조회합니다.
     * Batch 엔티티 대신 BookBatchTarget DTO Projection을 반환합니다.
     *
     * @param status 보강 상태 (PENDING)
     * @param pageable 페이지 정보
     * @return BookBatchTarget DTO Projection의 Page
     */
    @Query("SELECT new com.nhnacademy.book_data_batch.batch.core.dto.BookBatchTarget(" +
           "b.book.id, b.book.isbn, b.id) " +
           "FROM Batch b JOIN b.book WHERE b.enrichmentStatus = :status")
    Page<BookBatchTarget> findPendingBatches(@Param("status") BatchStatus status, Pageable pageable);

    /**
     * 임베딩이 필요한 Batch 대상을 Page 형태로 조회합니다.
     * Batch 엔티티 대신 BookEmbeddingTarget DTO Projection을 반환합니다.
     * Publisher는 Processor에서 별도로 조회합니다.
     *
     * @param embeddingStatus 임베딩 상태 (PENDING)
     * @param enrichmentStatus 보강 상태 (COMPLETED)
     * @param pageable 페이지 정보
     * @return BookEmbeddingTarget DTO Projection의 Page
     */
    @Query("SELECT new com.nhnacademy.book_data_batch.batch.domain.embedding.dto.BookEmbeddingTarget(" +
           "bk.id, b.id, bk.isbn, bk.title, bk.description, " +
           "'', bk.priceSales, bk.stock, " +
           "'', '') " + // publisher, authors, tags는 Processor에서 별도 조회
           "FROM Batch b JOIN b.book bk WHERE b.embeddingStatus = :embeddingStatus AND b.enrichmentStatus = :enrichmentStatus")
    Page<BookEmbeddingTarget> findPendingEmbeddingBatches(
            @Param("embeddingStatus") BatchStatus embeddingStatus,
            @Param("enrichmentStatus") BatchStatus enrichmentStatus,
            Pageable pageable);

    /**
     * 임베딩이 필요한 Batch 개수를 조회합니다.
     *
     * @param embeddingStatus 임베딩 상태 (PENDING)
     * @param enrichmentStatus 보강 상태 (COMPLETED)
     * @return Batch 개수
     */
    @Query("SELECT COUNT(b) FROM Batch b " +
           "WHERE b.embeddingStatus = :embeddingStatus AND b.enrichmentStatus = :enrichmentStatus")
    long countByEmbeddingStatusAndEnrichmentStatus(
            @Param("embeddingStatus") BatchStatus embeddingStatus,
            @Param("enrichmentStatus") BatchStatus enrichmentStatus);
}
