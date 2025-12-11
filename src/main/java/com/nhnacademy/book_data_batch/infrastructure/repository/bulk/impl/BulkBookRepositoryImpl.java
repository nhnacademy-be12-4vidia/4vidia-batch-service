package com.nhnacademy.book_data_batch.infrastructure.repository.bulk.impl;

import com.nhnacademy.book_data_batch.batch.components.provider.aladin.dto.EnrichmentSuccessDto;
import com.nhnacademy.book_data_batch.infrastructure.jdbc.JdbcExecutor;
import com.nhnacademy.book_data_batch.domain.Book;
import com.nhnacademy.book_data_batch.infrastructure.repository.bulk.BulkBookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.Date;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class BulkBookRepositoryImpl implements BulkBookRepository {

    private final JdbcExecutor bulkExecutor;

    private static final String INSERT_BOOK_SQL = """
            INSERT IGNORE INTO book (
                isbn_13, title, description, publisher_id, published_date,
                price_standard, price_sales, category_id, volume_number,
                stock, stock_status, packaging_available
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_ENRICHED_FIELDS_SQL = """
            UPDATE book SET
                description = ?,
                subtitle = ?,
                book_index = ?,
                page_count = ?,
                price_standard = ?,
                price_sales = ?,
                published_date = ?,
                language = ?
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
                    ps.setObject(6, book.getPriceStandard() != null
                            ? (int)(book.getPriceStandard() * 0.9) : null); // 10% 할인 판매가
                    ps.setObject(7, book.getPriceSales());
                    ps.setObject(8, book.getCategory() != null ? book.getCategory().getId() : null);
                    ps.setObject(9, book.getVolumeNumber() != null ? book.getVolumeNumber() : 1);
                    ps.setInt(10, 0);  // stock 기본값
                    ps.setInt(11, 0);  // stock_status 기본값 (PRE_ORDER)
                    ps.setBoolean(12, true);  // packaging_available 기본값
                }
        );
    }

    @Override
    public void bulkUpdateFromEnrichment(List<EnrichmentSuccessDto> enrichmentData) {
        if (enrichmentData.isEmpty()) {
            return;
        }

        bulkExecutor.execute(
                UPDATE_ENRICHED_FIELDS_SQL,
                enrichmentData,
                (ps, data) -> {
                    ps.setString(1, data.description());
                    ps.setString(2, data.subtitle());
                    ps.setString(3, data.bookIndex());
                    ps.setObject(4, data.pageCount());
                    ps.setObject(5, data.priceStandard());
                    ps.setObject(6, data.priceStandard() != null
                            ? (int)(data.priceStandard() * 0.9) : null); // 10% 할인 판매가
                    ps.setObject(7, data.publishedDate() != null
                            ? Date.valueOf(data.publishedDate()) : null);
                    ps.setString(8, data.language());
                    ps.setLong(9, data.bookId());
                }
        );
    }
}
