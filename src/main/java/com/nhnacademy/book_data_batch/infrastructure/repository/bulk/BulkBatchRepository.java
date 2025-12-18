package com.nhnacademy.book_data_batch.infrastructure.repository.bulk;

import com.nhnacademy.book_data_batch.batch.domain.aladin.dto.EnrichmentFailureDto;
import com.nhnacademy.book_data_batch.batch.domain.embedding.dto.EmbeddingFailureDto;
import com.nhnacademy.book_data_batch.domain.Batch;
import com.nhnacademy.book_data_batch.domain.enums.BatchStatus;

import java.util.List;

public interface BulkBatchRepository {

    void bulkInsert(List<Batch> batches);

    // Enrichment (Aladin)
    void bulkUpdateEnrichmentStatus(List<Long> batchIds, BatchStatus status);
    void bulkUpdateEnrichmentFailed(List<EnrichmentFailureDto> failedBatches);

    // Embedding
    void bulkUpdateEmbeddingStatus(List<Long> batchIds, BatchStatus status);
    void bulkUpdateEmbeddingFailed(List<EmbeddingFailureDto> failedBatches);

    // Cleanup
    void deleteAllCompleted();
}
