package com.nhnacademy.book_data_batch.infrastructure.repository.bulk.impl;

import com.nhnacademy.book_data_batch.infrastructure.jdbc.BulkJdbcExecutor;
import com.nhnacademy.book_data_batch.infrastructure.repository.bulk.BulkPublisherRepository;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@RequiredArgsConstructor
public class BulkPublisherRepositoryImpl implements BulkPublisherRepository {

    private final BulkJdbcExecutor bulkExecutor;

    private static final String INSERT_SQL = "INSERT IGNORE INTO publisher (publisher_name) VALUES (?)";

    @Override
    public void bulkInsert(Set<String> publisherNames) {
        bulkExecutor.execute(
                INSERT_SQL,
                publisherNames,
                (ps, name) -> ps.setString(1, name)
        );
    }
}
