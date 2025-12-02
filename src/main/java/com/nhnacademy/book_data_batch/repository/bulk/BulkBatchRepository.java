package com.nhnacademy.book_data_batch.repository.bulk;

import com.nhnacademy.book_data_batch.entity.Batch;
import com.nhnacademy.book_data_batch.entity.enums.BatchStatus;

import java.util.List;

public interface BulkBatchRepository {

    void bulkInsert(List<Batch> batches);

    void bulkUpdateEnrichmentStatus(List<Long> batchIds, BatchStatus status);

    void bulkUpdateEnrichmentFailed(List<Object[]> failedBatches);
}
