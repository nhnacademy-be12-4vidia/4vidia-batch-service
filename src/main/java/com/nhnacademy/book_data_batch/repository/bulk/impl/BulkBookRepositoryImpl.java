package com.nhnacademy.book_data_batch.repository.bulk.impl;

import com.nhnacademy.book_data_batch.common.jdbc.BulkJdbcExecutor;
import com.nhnacademy.book_data_batch.entity.Book;
import com.nhnacademy.book_data_batch.repository.bulk.BulkBookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.Date;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class BulkBookRepositoryImpl implements BulkBookRepository {

    private final BulkJdbcExecutor bulkExecutor;

    private static final String INSERT_BOOK_SQL = """
            INSERT IGNORE INTO book (
                isbn_13, title, description, publisher_id, published_date,
                price_standard, price_sales, category_id, volume_number, raw_author,
                stock, stock_status, packaging_available
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

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
    public void bulkInsert(List<Book> books) {
        if (books.isEmpty()) {
            return;
        }

        bulkExecutor.execute(
                INSERT_BOOK_SQL,
                books,
                (ps, book) -> {
                    ps.setString(1, book.getIsbn());
                    ps.setString(2, book.getTitle());
                    ps.setString(3, book.getDescription());
                    ps.setObject(4, book.getPublisher() != null ? book.getPublisher().getId() : null);
                    ps.setObject(5, book.getPublishedDate() != null 
                            ? Date.valueOf(book.getPublishedDate()) : null);
                    ps.setObject(6, book.getPriceStandard());
                    ps.setObject(7, book.getPriceSales());
                    ps.setObject(8, book.getCategory() != null ? book.getCategory().getId() : null);
                    ps.setObject(9, book.getVolumeNumber() != null ? book.getVolumeNumber() : 1);
                    ps.setString(10, book.getRawAuthor());
                    ps.setInt(11, 0);  // stock 기본값
                    ps.setInt(12, 0);  // stock_status 기본값 (PRE_ORDER)
                    ps.setBoolean(13, true);  // packaging_available 기본값
                }
        );
    }

    @Override
    public void bulkUpdate(List<Book> books) {
        if (books.isEmpty()) {
            return;
        }

        bulkExecutor.execute(
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
    }
}
