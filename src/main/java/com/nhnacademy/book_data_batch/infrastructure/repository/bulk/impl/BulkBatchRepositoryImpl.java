package com.nhnacademy.book_data_batch.infrastructure.repository.bulk.impl;

import com.nhnacademy.book_data_batch.batch.core.dto.BookBatchTarget;
import com.nhnacademy.book_data_batch.batch.domain.embedding.dto.BookEmbeddingTarget;
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

    // QueryDSL 조회 (JDBC)
    private static final String FIND_PENDING_ENRICHMENT_SQL = """
            SELECT b.book_id, b.isbn_13, batch.batch_id
            FROM batch
            INNER JOIN book b ON batch.book_id = b.book_id
            WHERE batch.enrichment_status = ?
            ORDER BY b.published_date DESC
            """;

    // Embedding 대상 조회 (Pending 상태)
    // - Aladin Step을 건너뛰거나 재작업 시 사용
    // - authors, tags는 별도 조회 필요 (여기선 빈 값)
    private static final String FIND_PENDING_EMBEDDING_SQL = """
            SELECT
                b.book_id, batch.batch_id, b.isbn_13, b.title, b.description,
                p.publisher_name, b.price_sales, b.stock
            FROM batch
            INNER JOIN book b ON batch.book_id = b.book_id
            LEFT JOIN publisher p ON b.publisher_id = p.publisher_id
            WHERE batch.embedding_status = ?
            ORDER BY b.book_id ASC
            """;

    // JDBC 배치 분할 조회
    private static final String FIND_EMBEDDING_TARGETS_SQL = """
            SELECT
                b.book_id, batch.batch_id, b.isbn_13, b.title, b.description,
                p.publisher_name, b.price_sales, b.stock
            FROM batch
            INNER JOIN book b ON batch.book_id = b.book_id
            LEFT JOIN publisher p ON b.publisher_id = p.publisher_id
            WHERE b.book_id IN (%s)
            ORDER BY b.book_id ASC
            """;

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
    public List<BookBatchTarget> findPendingEnrichmentStatusBook() {
        return bulkExecutor.query(
                FIND_PENDING_ENRICHMENT_SQL,
                (rs, rowNum) -> new BookBatchTarget(
                        rs.getLong("book_id"),
                        rs.getString("isbn_13"),
                        rs.getLong("batch_id")
                ),
                BatchStatus.PENDING.getCode()
        );
    }

    @Override
    public List<BookEmbeddingTarget> findPendingEmbeddingStatusBook() {
        return bulkExecutor.query(
                FIND_PENDING_EMBEDDING_SQL,
                (rs, rowNum) -> new BookEmbeddingTarget(
                        rs.getLong("book_id"),
                        rs.getLong("batch_id"),
                        rs.getString("isbn_13"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("publisher_name"),
                        rs.getObject("price_sales") != null ? rs.getInt("price_sales") : null,
                        rs.getObject("stock") != null ? rs.getInt("stock") : null,
                        "",  // authors (별도 조회 필요)
                        ""   // tags (별도 조회 필요)
                ),
                BatchStatus.PENDING.getCode()
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
    public List<BookEmbeddingTarget> findEmbeddingTargetsByBookIdsWithBatching(List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return List.of();
        }

        // bookId를 String으로 변환 (queryInBatches는 Collection<String> 필요)
        List<String> bookIdStrings = bookIds.stream()
                .map(String::valueOf)
                .toList();

        // JDBC 배치 분할 조회 (1000개씩 분할)
        return bulkExecutor.queryInBatches(
                FIND_EMBEDDING_TARGETS_SQL,
                (rs, rowNum) -> new BookEmbeddingTarget(
                        rs.getLong("book_id"),
                        rs.getLong("batch_id"),
                        rs.getString("isbn_13"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("publisher_name"),
                        rs.getObject("price_sales") != null ? rs.getInt("price_sales") : null,
                        rs.getObject("stock") != null ? rs.getInt("stock") : null,
                        "",  // authors는 이후 Aladin 정보로 병합
                        ""   // tags는 이후 Aladin 정보로 병합
                ),
                bookIdStrings,
                1000  // IN 절 1000개씩 분할
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
