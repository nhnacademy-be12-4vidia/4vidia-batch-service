package com.nhnacademy.book_data_batch.infrastructure.repository.bulk.impl;

import com.nhnacademy.book_data_batch.infrastructure.jdbc.BulkJdbcExecutor;
import com.nhnacademy.book_data_batch.infrastructure.repository.bulk.BulkAuthorRepository;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@RequiredArgsConstructor
public class BulkAuthorRepositoryImpl implements BulkAuthorRepository {

    private final BulkJdbcExecutor bulkExecutor;

    private static final String INSERT_SQL = "INSERT IGNORE INTO author (author_name) VALUES (?)";

    @Override
    public void bulkInsert(Set<String> authorNames) {
        bulkExecutor.execute(
                INSERT_SQL,
                authorNames,
                (ps, name) -> ps.setString(1, name)
        );
    }
}
