package com.nhnacademy.book_data_batch.domain.repository;

import com.nhnacademy.book_data_batch.domain.entity.Batch;
import com.nhnacademy.book_data_batch.domain.enums.BatchStatus;
import com.nhnacademy.book_data_batch.domain.repository.custom.BatchRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BatchRepository extends JpaRepository<Batch, Long>, BatchRepositoryCustom {

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
