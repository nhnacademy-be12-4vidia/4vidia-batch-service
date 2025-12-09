package com.nhnacademy.book_data_batch.infrastructure.repository.bulk.impl;

import com.nhnacademy.book_data_batch.infrastructure.jdbc.JdbcExecutor;
import com.nhnacademy.book_data_batch.domain.Batch;
import com.nhnacademy.book_data_batch.domain.enums.BatchStatus;
import com.nhnacademy.book_data_batch.infrastructure.repository.bulk.BulkBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class BulkBatchRepositoryImpl implements BulkBatchRepository {

    private final JdbcExecutor bulkExecutor;

    private static final String INSERT_BATCH_SQL = """
            INSERT IGNORE INTO batch (book_id, enrichment_status, embedding_status, error_message)
            VALUES (?, ?, ?, NULL)
            """;

    // Enrichment
    private static final String UPDATE_ENRICHMENT_STATUS_SQL = 
            "UPDATE batch SET enrichment_status = ? WHERE batch_id = ?";

    private static final String UPDATE_ENRICHMENT_FAILED_SQL = 
            "UPDATE batch SET enrichment_status = ?, error_message = ? WHERE batch_id = ?";

    // Embedding
    private static final String UPDATE_EMBEDDING_STATUS_SQL = 
            "UPDATE batch SET embedding_status = ? WHERE batch_id = ?";

    private static final String UPDATE_EMBEDDING_FAILED_SQL = 
            "UPDATE batch SET embedding_status = ?, error_message = ? WHERE batch_id = ?";

    // Cleanup
    private static final String DELETE_COMPLETED_SQL = """
            DELETE FROM batch 
            WHERE enrichment_status = ? AND embedding_status = ?
            """;

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
                    ps.setInt(3, BatchStatus.PENDING.getCode());
                }
        );
    }

    @Override
    public void bulkUpdateEnrichmentStatus(List<Long> batchIds, BatchStatus status) {
        if (batchIds.isEmpty()) {
            return;
        }

        bulkExecutor.execute(
                UPDATE_ENRICHMENT_STATUS_SQL,
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
                UPDATE_ENRICHMENT_FAILED_SQL,
                failedBatches,
                (ps, data) -> {
                    ps.setInt(1, BatchStatus.PENDING.getCode());  // PENDING 유지 (재시도 가능)
                    ps.setString(2, truncateMessage((String) data[1]));
                    ps.setLong(3, (Long) data[0]);
                }
        );
    }

    @Override
    public void bulkUpdateEmbeddingStatus(List<Long> batchIds, BatchStatus status) {
        if (batchIds.isEmpty()) {
            return;
        }

        bulkExecutor.execute(
                UPDATE_EMBEDDING_STATUS_SQL,
                batchIds,
                (ps, batchId) -> {
                    ps.setInt(1, status.getCode());
                    ps.setLong(2, batchId);
                }
        );
    }

    @Override
    public void bulkUpdateEmbeddingFailed(List<Object[]> failedBatches) {
        if (failedBatches.isEmpty()) {
            return;
        }

        bulkExecutor.execute(
                UPDATE_EMBEDDING_FAILED_SQL,
                failedBatches,
                (ps, data) -> {
                    ps.setInt(1, BatchStatus.PENDING.getCode());  // PENDING 유지 (재시도 가능)
                    ps.setString(2, truncateMessage((String) data[1]));
                    ps.setLong(3, (Long) data[0]);
                }
        );
    }

    @Override
    public void deleteAllCompleted() {
        bulkExecutor.executeUpdate(
                DELETE_COMPLETED_SQL,
                ps -> {
                    ps.setInt(1, BatchStatus.COMPLETED.getCode());
                    ps.setInt(2, BatchStatus.COMPLETED.getCode());
                }
        );
        log.info("[BATCH] 완료된 Batch 레코드 삭제 완료");
    }

    private String truncateMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
