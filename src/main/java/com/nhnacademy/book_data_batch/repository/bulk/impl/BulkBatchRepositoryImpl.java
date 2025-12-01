package com.nhnacademy.book_data_batch.repository.bulk.impl;

import com.nhnacademy.book_data_batch.entity.enums.BatchStatus;
import com.nhnacademy.book_data_batch.repository.bulk.BulkBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class BulkBatchRepositoryImpl implements BulkBatchRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String UPDATE_STATUS_SQL = 
            "UPDATE batch SET enrichment_status = ? WHERE batch_id = ?";

    private static final String UPDATE_FAILED_SQL = 
            "UPDATE batch SET enrichment_status = ?, error_message = ?, retry_count = retry_count + 1 WHERE batch_id = ?";

    @Override
    public void bulkUpdateEnrichmentStatus(List<Long> batchIds, BatchStatus status) {
        if (batchIds.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(UPDATE_STATUS_SQL, batchIds, batchIds.size(),
                (ps, batchId) -> {
                    ps.setInt(1, status.getCode());
                    ps.setLong(2, batchId);
                });

        log.debug("Batch enrichmentStatus 업데이트 완료: count={}, status={}", batchIds.size(), status);
    }

    @Override
    public void bulkUpdateEnrichmentFailed(List<Object[]> failedBatches) {
        if (failedBatches.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(UPDATE_FAILED_SQL, failedBatches, failedBatches.size(),
                (ps, data) -> {
                    ps.setInt(1, BatchStatus.FAILED.getCode());
                    ps.setString(2, truncateMessage((String) data[1]));  // errorMessage
                    ps.setLong(3, (Long) data[0]);  // batchId
                });

        log.debug("Batch 실패 업데이트 완료: count={}", failedBatches.size());
    }

    private String truncateMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
