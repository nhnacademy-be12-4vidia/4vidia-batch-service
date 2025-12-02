package com.nhnacademy.book_data_batch.repository.bulk.impl;

import com.nhnacademy.book_data_batch.common.jdbc.BulkJdbcExecutor;
import com.nhnacademy.book_data_batch.repository.bulk.BulkTagRepository;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@RequiredArgsConstructor
public class BulkTagRepositoryImpl implements BulkTagRepository {

    private final BulkJdbcExecutor bulkExecutor;

    private static final String INSERT_SQL = "INSERT IGNORE INTO tag (tag_name) VALUES (?)";

    @Override
    public void bulkInsert(Set<String> tagNames) {
        bulkExecutor.execute(
                INSERT_SQL,
                tagNames,
                (ps, name) -> ps.setString(1, name)
        );
    }
}
