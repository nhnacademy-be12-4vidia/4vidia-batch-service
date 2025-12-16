package com.nhnacademy.book_data_batch.infrastructure.repository.bulk;

import com.nhnacademy.book_data_batch.batch.core.dto.BookBatchTarget;
import com.nhnacademy.book_data_batch.batch.domain.aladin.dto.EnrichmentFailureDto;
import com.nhnacademy.book_data_batch.batch.domain.embedding.dto.BookEmbeddingTarget;
import com.nhnacademy.book_data_batch.batch.domain.embedding.dto.EmbeddingFailureDto;
import com.nhnacademy.book_data_batch.domain.Batch;
import com.nhnacademy.book_data_batch.domain.enums.BatchStatus;

import java.util.List;

public interface BulkBatchRepository {

    void bulkInsert(List<Batch> batches);

    // QueryDSL 조회
    List<BookBatchTarget> findPendingEnrichmentStatusBook();

    // Embedding 대상 조회 (Pending 상태)
    List<BookEmbeddingTarget> findPendingEmbeddingStatusBook();

    // Enrichment (Aladin)
    void bulkUpdateEnrichmentStatus(List<Long> batchIds, BatchStatus status);
    void bulkUpdateEnrichmentFailed(List<EnrichmentFailureDto> failedBatches);

    // Embedding
    void bulkUpdateEmbeddingStatus(List<Long> batchIds, BatchStatus status);
    void bulkUpdateEmbeddingFailed(List<EmbeddingFailureDto> failedBatches);

    // JDBC 배치 분할 조회
    List<BookEmbeddingTarget> findEmbeddingTargetsByBookIdsWithBatching(List<Long> bookIds);

    // Cleanup
    void deleteAllCompleted();
}
