package com.nhnacademy.book_data_batch.infrastructure.repository.bulk.impl;

import com.nhnacademy.book_data_batch.batch.components.domain.book.dto.BookImageDto;
import com.nhnacademy.book_data_batch.infrastructure.jdbc.JdbcExecutor;
import com.nhnacademy.book_data_batch.infrastructure.repository.bulk.BulkBookImageRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class BulkBookImageRepositoryImpl implements BulkBookImageRepository {

    private final JdbcExecutor bulkExecutor;

    private static final String INSERT_SQL =
            "INSERT IGNORE INTO book_image (book_id, image_url, image_type) VALUES (?, ?, ?)";

    @Override
    public void bulkInsert(List<BookImageDto> bookImages) {
        bulkExecutor.execute(
                INSERT_SQL,
                bookImages,
                (ps, dto) -> {
                    ps.setLong(1, dto.bookId());
                    ps.setString(2, dto.imageUrl());
                    ps.setInt(3, dto.imageType());
                }
        );
    }
}
