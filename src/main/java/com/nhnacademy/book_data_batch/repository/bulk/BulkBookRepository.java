package com.nhnacademy.book_data_batch.repository.bulk;

import com.nhnacademy.book_data_batch.entity.Book;

import java.util.List;

/**
 * Book 엔티티 Bulk 작업용 Repository
 * - Insert, Update 등 대량 작업 지원
 */
public interface BulkBookRepository {

    /**
     * Aladin API로 보강된 필드들을 Bulk Update
     * (description, subtitle, bookIndex, pageCount, priceStandard, priceSales, publishedDate)
     *
     * @param books 업데이트할 Book 목록
     */
    void bulkUpdateEnrichedFields(List<Book> books);
}
