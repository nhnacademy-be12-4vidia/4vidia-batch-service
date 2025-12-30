package com.nhnacademy.book_data_batch.domain.repository.custom;

import com.nhnacademy.book_data_batch.jobs.aladin.dto.EnrichmentFailureDto;
import com.nhnacademy.book_data_batch.jobs.embedding.dto.EmbeddingFailureDto;
import com.nhnacademy.book_data_batch.domain.entity.Batch;
import com.nhnacademy.book_data_batch.domain.enums.BatchStatus;

import java.util.List;

public interface BatchRepositoryCustom {

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
