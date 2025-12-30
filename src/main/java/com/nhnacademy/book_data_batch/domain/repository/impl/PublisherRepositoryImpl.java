package com.nhnacademy.book_data_batch.domain.repository.impl;

import com.nhnacademy.book_data_batch.infrastructure.jdbc.JdbcExecutor;
import com.nhnacademy.book_data_batch.domain.repository.custom.PublisherRepositoryCustom;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@RequiredArgsConstructor
public class PublisherRepositoryImpl implements PublisherRepositoryCustom {

    private final JdbcExecutor bulkExecutor;

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
