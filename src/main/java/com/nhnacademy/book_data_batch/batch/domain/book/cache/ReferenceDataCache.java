package com.nhnacademy.book_data_batch.batch.domain.book.cache;

import com.nhnacademy.book_data_batch.batch.domain.book.dto.BookCsvRow;
import com.nhnacademy.book_data_batch.domain.Book;
import com.nhnacademy.book_data_batch.domain.Category;
import com.nhnacademy.book_data_batch.domain.Publisher;
import com.nhnacademy.book_data_batch.infrastructure.repository.BookRepository;
import com.nhnacademy.book_data_batch.infrastructure.repository.CategoryRepository;
import com.nhnacademy.book_data_batch.infrastructure.repository.PublisherRepository;

import java.util.Collection;
import java.util.List;

/**
 * 참조 데이터 캐시 인터페이스
 * - Publisher: CSV에서 추출 후 캐시
 * - Category: DB 전체 조회 후 캐시
 * - Book: Bulk INSERT 후 ISBN으로 캐시
 * - CSV 데이터: 메모리에 전체 로드
 */
public interface ReferenceDataCache {

    // Publisher 캐시
    Publisher findPublisher(String publisherName);
    void buildPublisherCache(PublisherRepository publisherRepository);
    int getPublisherCacheSize();

    // Category 캐시
    Category findCategory(String kdcCode);
    void buildCategoryCache(CategoryRepository categoryRepository);
    int getCategoryCacheSize();

    // Book 캐시
    Book findBook(String isbn13);
    void buildBookCache(BookRepository bookRepository, Collection<String> isbns);
    Collection<Book> getAllBooks();
    int getBookCacheSize();

    // CSV 데이터 캐시
    void setCsvData(List<BookCsvRow> csvData);
    List<BookCsvRow> getCsvData();
    int getCsvDataSize();

    // 공통
    void clear();
    boolean isReady();
}
