package com.nhnacademy.book_data_batch.batch.components.domain.book.cache;

import com.nhnacademy.book_data_batch.batch.components.domain.book.dto.BookCsvRow;
import com.nhnacademy.book_data_batch.domain.Book;
import com.nhnacademy.book_data_batch.domain.Category;
import com.nhnacademy.book_data_batch.domain.Publisher;
import com.nhnacademy.book_data_batch.infrastructure.repository.BookRepository;
import com.nhnacademy.book_data_batch.infrastructure.repository.CategoryRepository;
import com.nhnacademy.book_data_batch.infrastructure.repository.PublisherRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 참조 데이터 캐시 구현체 (In-Memory)
 * 
 * Step 1: CSV 로드 + Publisher/Category 캐시
 * Step 2: Book 변환 + Bulk INSERT + Book 캐시
 * Step 3: BookImage/Batch 저장 (Book 캐시 사용)
 */
@Slf4j
@Component
public class InMemoryReferenceDataCache implements ReferenceDataCache {

    private final Map<String, Publisher> publisherCache = new ConcurrentHashMap<>();
    private final Map<String, Category> categoryCache = new ConcurrentHashMap<>();
    private final Map<String, Book> bookCache = new ConcurrentHashMap<>();
    private List<BookCsvRow> csvDataCache = Collections.emptyList();
    private final AtomicBoolean ready = new AtomicBoolean(false);


    // Publisher 캐시

    @Override
    public Publisher findPublisher(String publisherName) {
        if (publisherName == null || publisherName.isBlank()) {
            return null;
        }
        return publisherCache.get(normalizeKey(publisherName));
    }

    @Override
    public void buildPublisherCache(PublisherRepository publisherRepository) {
        log.info("[Cache] Publisher 캐시 구축 시작...");
        long startTime = System.currentTimeMillis();

        publisherRepository.findAll().forEach(publisher -> {
            if (publisher.getName() != null) {
                publisherCache.put(normalizeKey(publisher.getName()), publisher);
            }
        });

        log.info("[Cache] Publisher 캐시 구축 완료: {}개, {}ms",
                publisherCache.size(), System.currentTimeMillis() - startTime);
    }

    @Override
    public int getPublisherCacheSize() {
        return publisherCache.size();
    }


    // Category 캐시

    private static final String UNCATEGORIZED_CODE = "UNC";

    @Override
    public Category findCategory(String kdcCode) {
        if (kdcCode == null || kdcCode.isBlank()) {
            return categoryCache.get(UNCATEGORIZED_CODE);  // 미분류 반환
        }
        Category category = categoryCache.get(kdcCode.trim());
        return category != null ? category : categoryCache.get(UNCATEGORIZED_CODE);  // 없으면 미분류
    }

    @Override
    public void buildCategoryCache(CategoryRepository categoryRepository) {
        log.info("[Cache] Category 캐시 구축 시작...");
        long startTime = System.currentTimeMillis();

        categoryRepository.findAll().forEach(category -> {
            if (category.getKdcCode() != null) {
                categoryCache.put(category.getKdcCode().trim(), category);
            }
        });

        log.info("[Cache] Category 캐시 구축 완료: {}개, {}ms",
                categoryCache.size(), System.currentTimeMillis() - startTime);
    }

    @Override
    public int getCategoryCacheSize() {
        return categoryCache.size();
    }


    // Book 캐시

    @Override
    public Book findBook(String isbn13) {
        if (isbn13 == null || isbn13.isBlank()) {
            return null;
        }
        return bookCache.get(isbn13.trim());
    }

    @Override
    public void buildBookCache(BookRepository bookRepository, Collection<String> isbns) {
        log.info("[Cache] Book 캐시 구축 시작... (ISBN {}개)", isbns.size());
        long startTime = System.currentTimeMillis();

        bookRepository.findAllByIsbnIn(isbns).forEach(book -> {
            if (book.getIsbn() != null) {
                bookCache.put(book.getIsbn(), book);
            }
        });

        log.info("[Cache] Book 캐시 구축 완료: {}개, {}ms",
                bookCache.size(), System.currentTimeMillis() - startTime);
    }

    @Override
    public Collection<Book> getAllBooks() {
        return bookCache.values();
    }

    @Override
    public int getBookCacheSize() {
        return bookCache.size();
    }


    // CSV 데이터 캐시

    @Override
    public void setCsvData(List<BookCsvRow> csvData) {
        this.csvDataCache = csvData != null ? new ArrayList<>(csvData) : Collections.emptyList();
        log.info("[Cache] CSV 데이터 캐시 완료: {}건", csvDataCache.size());
    }

    @Override
    public List<BookCsvRow> getCsvData() {
        return csvDataCache;
    }

    @Override
    public int getCsvDataSize() {
        return csvDataCache.size();
    }


    // 공통

    @Override
    public void clear() {
        publisherCache.clear();
        categoryCache.clear();
        bookCache.clear();
        csvDataCache = Collections.emptyList();
        ready.set(false);
        log.info("[Cache] 캐시 초기화 완료");
    }

    @Override
    public boolean isReady() {
        return ready.get();
    }

    public void markReady() {
        ready.set(true);
    }

    private String normalizeKey(String key) {
        return key.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
