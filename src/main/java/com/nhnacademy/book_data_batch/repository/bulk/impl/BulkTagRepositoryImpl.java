package com.nhnacademy.book_data_batch.repository.bulk.impl;

import com.nhnacademy.book_data_batch.common.jdbc.BulkInsertHelper;
import com.nhnacademy.book_data_batch.repository.bulk.BulkTagRepository;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@RequiredArgsConstructor
public class BulkTagRepositoryImpl implements BulkTagRepository {

    private final BulkInsertHelper bulkInsertHelper;

    private static final String INSERT_SQL = "INSERT IGNORE INTO tag (tag_name) VALUES (?)";

    @Override
    public void bulkInsert(Set<String> tagNames) {
        bulkInsertHelper.bulkInsert(
                INSERT_SQL,
                tagNames,
                (ps, name) -> ps.setString(1, name)
        );
    }
}
