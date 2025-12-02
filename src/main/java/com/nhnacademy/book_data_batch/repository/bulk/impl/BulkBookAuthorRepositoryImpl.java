package com.nhnacademy.book_data_batch.repository.bulk.impl;

import com.nhnacademy.book_data_batch.batch.aladin.dto.BookAuthorDto;
import com.nhnacademy.book_data_batch.common.jdbc.BulkJdbcExecutor;
import com.nhnacademy.book_data_batch.repository.bulk.BulkBookAuthorRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class BulkBookAuthorRepositoryImpl implements BulkBookAuthorRepository {

    private final BulkJdbcExecutor bulkExecutor;

    private static final String INSERT_SQL =
            "INSERT IGNORE INTO book_author (book_id, author_id, author_role) VALUES (?, ?, ?)";

    @Override
    public void bulkInsert(List<BookAuthorDto> bookAuthors) {
        bulkExecutor.execute(
                INSERT_SQL,
                bookAuthors,
                (ps, dto) -> {
                    ps.setLong(1, dto.bookId());
                    ps.setLong(2, dto.authorId());
                    ps.setString(3, dto.role());
                }
        );
    }
}
