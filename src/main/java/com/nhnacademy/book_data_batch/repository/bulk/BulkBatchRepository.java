package com.nhnacademy.book_data_batch.repository.bulk;

import com.nhnacademy.book_data_batch.entity.enums.BatchStatus;

import java.util.List;

public interface BulkBatchRepository {

    void bulkUpdateEnrichmentStatus(List<Long> batchIds, BatchStatus status);

    void bulkUpdateEnrichmentFailed(List<Object[]> failedBatches);
}
