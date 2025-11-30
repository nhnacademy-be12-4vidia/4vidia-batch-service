package com.nhnacademy.book_data_batch.repository.bulk.impl;

import com.nhnacademy.book_data_batch.common.jdbc.BulkInsertHelper;
import com.nhnacademy.book_data_batch.repository.bulk.BulkAuthorRepository;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@RequiredArgsConstructor
public class BulkAuthorRepositoryImpl implements BulkAuthorRepository {

    private final BulkInsertHelper bulkInsertHelper;

    private static final String INSERT_SQL = "INSERT IGNORE INTO author (author_name) VALUES (?)";

    @Override
    public void bulkInsert(Set<String> authorNames) {
        bulkInsertHelper.bulkInsert(
                INSERT_SQL,
                authorNames,
                (ps, name) -> ps.setString(1, name)
        );
    }
}
