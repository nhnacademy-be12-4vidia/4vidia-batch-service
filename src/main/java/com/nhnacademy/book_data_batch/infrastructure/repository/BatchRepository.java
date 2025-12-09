package com.nhnacademy.book_data_batch.infrastructure.repository;

import com.nhnacademy.book_data_batch.domain.Batch;
import com.nhnacademy.book_data_batch.domain.enums.BatchStatus;
import com.nhnacademy.book_data_batch.infrastructure.repository.bulk.BulkBatchRepository;
import com.nhnacademy.book_data_batch.infrastructure.repository.querydsl.QuerydslBatchRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Batch 엔티티 Repository
 * - JPA 기본 메소드: JpaRepository
 * - 복잡한 쿼리 (MIN/MAX, JOIN + Projection): QuerydslBatchRepository (QueryDSL)
 * - Bulk 연산: BulkBatchRepository
 */
public interface BatchRepository extends JpaRepository<Batch, Long>, BulkBatchRepository, QuerydslBatchRepository {
}
