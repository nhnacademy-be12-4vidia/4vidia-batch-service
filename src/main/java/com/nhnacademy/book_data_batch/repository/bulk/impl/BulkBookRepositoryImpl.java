package com.nhnacademy.book_data_batch.repository.bulk.impl;

import com.nhnacademy.book_data_batch.common.jdbc.BulkUpdateHelper;
import com.nhnacademy.book_data_batch.entity.Book;
import com.nhnacademy.book_data_batch.repository.bulk.BulkBookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.Date;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class BulkBookRepositoryImpl implements BulkBookRepository {

    private final BulkUpdateHelper bulkUpdateHelper;

    private static final String UPDATE_ENRICHED_FIELDS_SQL = """
            UPDATE book SET
                description = ?,
                subtitle = ?,
                book_index = ?,
                page_count = ?,
                price_standard = ?,
                price_sales = ?,
                published_date = ?
            WHERE book_id = ?
            """;

    @Override
    public void bulkUpdate(List<Book> books) {
        if (books.isEmpty()) {
            return;
        }

        bulkUpdateHelper.bulkUpdate(
                UPDATE_ENRICHED_FIELDS_SQL,
                books,
                (ps, book) -> {
                    ps.setString(1, book.getDescription());
                    ps.setString(2, book.getSubtitle());
                    ps.setString(3, book.getBookIndex());
                    ps.setObject(4, book.getPageCount());
                    ps.setObject(5, book.getPriceStandard());
                    ps.setObject(6, book.getPriceSales());
                    ps.setObject(7, book.getPublishedDate() != null 
                            ? Date.valueOf(book.getPublishedDate()) : null);
                    ps.setLong(8, book.getId());
                }
        );

        log.debug("Book 보강 필드 업데이트 완료: count={}", books.size());
    }
}
