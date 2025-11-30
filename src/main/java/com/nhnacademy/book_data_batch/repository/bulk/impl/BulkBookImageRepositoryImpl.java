package com.nhnacademy.book_data_batch.repository.bulk.impl;

import com.nhnacademy.book_data_batch.batch.book.dto.BookImageDto;
import com.nhnacademy.book_data_batch.common.jdbc.BulkInsertHelper;
import com.nhnacademy.book_data_batch.repository.bulk.BulkBookImageRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class BulkBookImageRepositoryImpl implements BulkBookImageRepository {

    private final BulkInsertHelper bulkInsertHelper;

    private static final String INSERT_SQL =
            "INSERT IGNORE INTO book_image (book_id, image_url, image_type) VALUES (?, ?, ?)";

    @Override
    public void bulkInsert(List<BookImageDto> bookImages) {
        bulkInsertHelper.bulkInsert(
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
