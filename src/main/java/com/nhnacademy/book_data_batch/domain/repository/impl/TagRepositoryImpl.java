package com.nhnacademy.book_data_batch.domain.repository.impl;

import com.nhnacademy.book_data_batch.infrastructure.jdbc.JdbcExecutor;
import com.nhnacademy.book_data_batch.domain.repository.custom.TagRepositoryCustom;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@RequiredArgsConstructor
public class TagRepositoryImpl implements TagRepositoryCustom {

    private final JdbcExecutor bulkExecutor;

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
