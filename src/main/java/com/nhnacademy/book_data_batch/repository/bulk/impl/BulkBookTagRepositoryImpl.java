package com.nhnacademy.book_data_batch.repository.bulk.impl;

import com.nhnacademy.book_data_batch.common.jdbc.BulkInsertHelper;
import com.nhnacademy.book_data_batch.repository.bulk.BulkBookTagRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class BulkBookTagRepositoryImpl implements BulkBookTagRepository {

    private final BulkInsertHelper bulkInsertHelper;

    private static final String INSERT_SQL = 
            "INSERT IGNORE INTO book_tag (book_id, tag_id) VALUES (?, ?)";

    @Override
    public void bulkInsert(List<long[]> bookTagPairs) {
        bulkInsertHelper.bulkInsert(
                INSERT_SQL,
                bookTagPairs,
                (ps, pair) -> {
                    ps.setLong(1, pair[0]);  // book_id
                    ps.setLong(2, pair[1]);  // tag_id
                }
        );
    }
}
