package com.nhnacademy.book_data_batch.infrastructure.jdbc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * JDBC 작업 유틸리티
 * - execute: Bulk INSERT/UPDATE (배치 분할, 재시도 로직 포함)
 * - executeUpdate: 단일 UPDATE/DELETE
 * - queryInBatches: 대량 조회 (IN 절 분할)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JdbcExecutor {

    private final JdbcTemplate jdbcTemplate;

    private static final int DEFAULT_BATCH_SIZE = 10000;

    /**
     * Bulk INSERT/UPDATE (기본 배치 사이즈 10000)
     */
    public <T> void execute(
            String sql,
            Collection<T> items,
            PreparedStatementSetter<T> setter
    ) {
        execute(sql, items, setter, DEFAULT_BATCH_SIZE);
    }

    /**
     * Bulk INSERT/UPDATE (커스텀 배치 사이즈)
     */
    public <T> void execute(
            String sql,
            Collection<T> items,
            PreparedStatementSetter<T> setter,
            int batchSize
    ) {
        if (items == null || items.isEmpty()) {
            return;
        }

        List<T> itemList = items instanceof List
                ? (List<T>) items
                : new ArrayList<>(items);

        for (int i = 0; i < itemList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, itemList.size());
            List<T> batch = itemList.subList(i, end);
            executeBatch(sql, batch, setter);
        }
    }

    @Retryable(
            retryFor = CannotAcquireLockException.class, // DB 락 획득 실패 시 재시도
            maxAttempts = 2,
            backoff = @Backoff(delay = 100)
    )
    private <T> void executeBatch(
            String sql,
            List<T> batch,
            PreparedStatementSetter<T> setter
    ) {
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                setter.setValues(ps, batch.get(i));
            }

            @Override
            public int getBatchSize() {
                return batch.size();
            }
        });
    }

    @Recover
    private <T> void recoverFromBatchFailure(CannotAcquireLockException e, String sql, List<T> batch, PreparedStatementSetter<T> setter) {
        log.error("Bulk 실행 최종 실패 - {}", e.getMessage());
        throw e;
    }

    /**
     * 단일 UPDATE/DELETE
     */
    public int executeUpdate(String sql, PreparedStatementParameterSetter setter) {
        return jdbcTemplate.update(sql, setter::setValues);
    }

    /**
     * SELECT (단순 조회)
     */
    public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
        return jdbcTemplate.query(sql, rowMapper, args);
    }

    /**
     * SELECT (IN 절 배치 분할 조회)
     * SQL 템플릿에 '%s' 플레이스홀더 필요: "SELECT * FROM table WHERE id IN (%s)"
     */
    public <T> List<T> queryInBatches(
            String sqlTemplate,
            RowMapper<T> rowMapper,
            Collection<String> names,
            int batchSize
    ) {
        if (names == null || names.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> list = names instanceof List
                ? (List<String>) names
                : new ArrayList<>(names);

        List<T> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            int end = Math.min(i + batchSize, list.size());
            List<String> chunk = list.subList(i, end);
            String placeholders = String.join(",", Collections.nCopies(chunk.size(), "?"));
            String sql = String.format(sqlTemplate, placeholders);
            result.addAll(jdbcTemplate.query(sql, rowMapper, chunk.toArray()));
        }

        return result;
    }

    @FunctionalInterface
    public interface PreparedStatementSetter<T> {
        void setValues(PreparedStatement ps, T item) throws SQLException;
    }

    @FunctionalInterface
    public interface PreparedStatementParameterSetter {
        void setValues(PreparedStatement ps) throws SQLException;
    }
}
