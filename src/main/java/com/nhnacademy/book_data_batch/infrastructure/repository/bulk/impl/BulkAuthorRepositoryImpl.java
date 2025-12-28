package com.nhnacademy.book_data_batch.infrastructure.repository.bulk.impl;

import com.nhnacademy.book_data_batch.infrastructure.jdbc.JdbcExecutor;
import com.nhnacademy.book_data_batch.infrastructure.repository.bulk.BulkAuthorRepository;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class BulkAuthorRepositoryImpl implements BulkAuthorRepository {

    private final JdbcExecutor bulkExecutor;

    private static final String INSERT_SQL = "INSERT IGNORE INTO author (author_name) VALUES (?)";
    private static final String SELECT_SQL_TEMPLATE = "SELECT author_id, author_name FROM author WHERE author_name IN (%s)";

    @Override
    public void bulkInsert(Set<String> authorNames) {
        bulkExecutor.execute(
                INSERT_SQL,
                authorNames,
                (ps, name) -> ps.setString(1, name)
        );
    }

    @Override
    public Map<String, Long> findIdsByNames(Set<String> names, int batchSize) {
        Map<String, Long> result = new HashMap<>();

        bulkExecutor.queryInBatches(
                SELECT_SQL_TEMPLATE,
                (rs, rowNum) -> {
                    result.put(rs.getString("author_name"), rs.getLong("author_id"));
                    return null;
                },
                names,
                batchSize
        );

        return result;
    }
}
