package com.nhnacademy.book_data_batch.repository;

import com.nhnacademy.book_data_batch.entity.Batch;
import com.nhnacademy.book_data_batch.entity.enums.BatchStatus;
import com.nhnacademy.book_data_batch.repository.bulk.BulkBatchRepository;
import com.nhnacademy.book_data_batch.repository.querydsl.BatchRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Batch 엔티티 Repository
 * - JPA 기본 메소드: JpaRepository
 * - 복잡한 쿼리 (MIN/MAX, JOIN + Projection): BatchRepositoryCustom (QueryDSL)
 * - Bulk 연산: BulkBatchRepository
 */
public interface BatchRepository extends JpaRepository<Batch, Long>, BulkBatchRepository, BatchRepositoryCustom {

    long countByEnrichmentStatus(BatchStatus status);
}
