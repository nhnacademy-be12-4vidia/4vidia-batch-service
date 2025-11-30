package com.nhnacademy.book_data_batch.common.jdbc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * JDBC Bulk Insert를 위한 공통 유틸리티 클래스
 * - 배치 분할, 재시도 로직, PreparedStatement 설정 기능 제공
 * - MySQL/MariaDB: INSERT IGNORE로 중복 무시 가능
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BulkInsertHelper {

    private final JdbcTemplate jdbcTemplate;

    private static final int DEFAULT_BATCH_SIZE = 10000;
    private static final int MAX_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 100;

    /**
     * Bulk Insert 실행 (기본 배치 사이즈 사용)
     *
     * @param sql    INSERT SQL
     * @param items  삽입할 아이템들
     * @param setter PreparedStatement에 값을 설정하는 함수
     * @param <T>    아이템 타입
     */
    public <T> void bulkInsert(
            String sql,
            Collection<T> items,
            PreparedStatementSetter<T> setter
    ) {
        bulkInsert(sql, items, setter, DEFAULT_BATCH_SIZE);
    }

    /**
     * Bulk Insert 실행 (커스텀 배치 사이즈)
     *
     * @param sql       INSERT SQL
     * @param items     삽입할 아이템들
     * @param setter    PreparedStatement에 값을 설정하는 함수
     * @param batchSize 배치 사이즈
     * @param <T>       아이템 타입
     */
    public <T> void bulkInsert(
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

            executeWithRetry(() -> executeBatch(sql, batch, setter));
        }
    }

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

    @SuppressWarnings("java:S2142")
    private void executeWithRetry(Runnable operation) {
        int attempt = 0;
        while (true) {
            try {
                operation.run();
                return;
            } catch (CannotAcquireLockException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    log.error("Bulk Insert 실패 - {}회 재시도 후 포기", MAX_RETRIES);
                    throw e;
                }
                log.warn("Deadlock/Lock timeout 발생 - {}회 재시도 중...", attempt);
                sleep(RETRY_DELAY_MS * attempt);
            }
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("재시도 중 인터럽트 발생", ie);
        }
    }

    @FunctionalInterface
    public interface PreparedStatementSetter<T> {
        void setValues(PreparedStatement ps, T item) throws SQLException;
    }
}
