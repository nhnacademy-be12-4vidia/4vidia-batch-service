package com.nhnacademy.book_data_batch.infrastructure.repository.bulk;

import com.nhnacademy.book_data_batch.batch.components.core.dto.BookBatchTarget;
import com.nhnacademy.book_data_batch.batch.components.domain.search.embedding.dto.BookEmbeddingTarget;
import com.nhnacademy.book_data_batch.domain.Batch;
import com.nhnacademy.book_data_batch.domain.enums.BatchStatus;

import java.util.List;

public interface BulkBatchRepository {

    void bulkInsert(List<Batch> batches);

    // QueryDSL 조회
    List<BookBatchTarget> findPendingEnrichmentStatusBook();

    // Enrichment (Aladin)
    void bulkUpdateEnrichmentStatus(List<Long> batchIds, BatchStatus status);
    void bulkUpdateEnrichmentFailed(List<Object[]> failedBatches);

    // Embedding
    void bulkUpdateEmbeddingStatus(List<Long> batchIds, BatchStatus status);
    void bulkUpdateEmbeddingFailed(List<Object[]> failedBatches);

    // JDBC 배치 분할 조회
    List<BookEmbeddingTarget> findEmbeddingTargetsByBookIdsWithBatching(List<Long> bookIds);

    // Cleanup
    void deleteAllCompleted();
}
