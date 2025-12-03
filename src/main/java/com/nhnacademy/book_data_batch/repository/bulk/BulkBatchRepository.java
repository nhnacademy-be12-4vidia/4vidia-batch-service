package com.nhnacademy.book_data_batch.repository.bulk;

import com.nhnacademy.book_data_batch.entity.Batch;
import com.nhnacademy.book_data_batch.entity.enums.BatchStatus;

import java.util.List;

public interface BulkBatchRepository {

    void bulkInsert(List<Batch> batches);

    // Enrichment (Aladin)
    void bulkUpdateEnrichmentStatus(List<Long> batchIds, BatchStatus status);
    void bulkUpdateEnrichmentFailed(List<Object[]> failedBatches);

    // Embedding
    void bulkUpdateEmbeddingStatus(List<Long> batchIds, BatchStatus status);
    void bulkUpdateEmbeddingFailed(List<Object[]> failedBatches);

    // Cleanup
    void deleteAllCompleted();
}
