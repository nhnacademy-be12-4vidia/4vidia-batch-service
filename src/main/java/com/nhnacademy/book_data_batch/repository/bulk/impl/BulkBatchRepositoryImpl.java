package com.nhnacademy.book_data_batch.repository.bulk.impl;

import com.nhnacademy.book_data_batch.common.jdbc.BulkJdbcExecutor;
import com.nhnacademy.book_data_batch.entity.Batch;
import com.nhnacademy.book_data_batch.entity.enums.BatchStatus;
import com.nhnacademy.book_data_batch.repository.bulk.BulkBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class BulkBatchRepositoryImpl implements BulkBatchRepository {

    private final BulkJdbcExecutor bulkExecutor;

    private static final String INSERT_BATCH_SQL = """
            INSERT IGNORE INTO batch (book_id, enrichment_status, error_message, retry_count)
            VALUES (?, ?, NULL, 0)
            """;

    private static final String UPDATE_STATUS_SQL = 
            "UPDATE batch SET enrichment_status = ? WHERE batch_id = ?";

    private static final String UPDATE_FAILED_SQL = 
            "UPDATE batch SET enrichment_status = ?, error_message = ?, retry_count = retry_count + 1 WHERE batch_id = ?";

    @Override
    public void bulkInsert(List<Batch> batches) {
        if (batches.isEmpty()) {
            return;
        }

        bulkExecutor.execute(
                INSERT_BATCH_SQL,
                batches,
                (ps, batch) -> {
                    ps.setLong(1, batch.getBook().getId());
                    ps.setInt(2, BatchStatus.PENDING.getCode());
                }
        );
    }

    @Override
    public void bulkUpdateEnrichmentStatus(List<Long> batchIds, BatchStatus status) {
        if (batchIds.isEmpty()) {
            return;
        }

        bulkExecutor.execute(
                UPDATE_STATUS_SQL,
                batchIds,
                (ps, batchId) -> {
                    ps.setInt(1, status.getCode());
                    ps.setLong(2, batchId);
                }
        );
    }

    @Override
    public void bulkUpdateEnrichmentFailed(List<Object[]> failedBatches) {
        if (failedBatches.isEmpty()) {
            return;
        }

        bulkExecutor.execute(
                UPDATE_FAILED_SQL,
                failedBatches,
                (ps, data) -> {
                    ps.setInt(1, BatchStatus.FAILED.getCode());
                    ps.setString(2, truncateMessage((String) data[1]));  // errorMessage
                    ps.setLong(3, (Long) data[0]);  // batchId
                }
        );
    }

    private String truncateMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
