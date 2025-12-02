package com.nhnacademy.book_data_batch.batch.book.writer;

import com.nhnacademy.book_data_batch.batch.book.cache.ReferenceDataCache;
import com.nhnacademy.book_data_batch.batch.book.dto.BookNormalizedItem;
import com.nhnacademy.book_data_batch.batch.book.tasklet.ReferenceDataLoadTasklet;
import com.nhnacademy.book_data_batch.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <pre>
 * BookItemWriter
 * - 도서 데이터 저장 (작가는 알라딘 API에서 처리)
 * 
 * 0. 청크 단위로 BookNormalizedItem 리스트 수신
 * 1. ReferenceDataCache에서 Publisher 조회
 * 2. Book, BookImage, Batch 엔티티 저장
 * 
 * @see ReferenceDataLoadTasklet Step 1에서 출판사 사전 로딩
 * @see BookPersistenceHelper 도서 영속화 담당
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional
public class BookItemWriter implements ItemWriter<BookNormalizedItem> {

    private final BookPersistenceHelper persistenceHelper;
    private final ReferenceDataCache referenceDataCache;

    /**
     * 청크 데이터 -> DB 저장
     * 
     * @param chunk 처리할 도서 데이터 청크
     * @throws Exception 저장 중 오류 발생 시
     */
    @Override
    public void write(Chunk<? extends BookNormalizedItem> chunk) throws Exception {

        if (chunk.isEmpty()) {
            return;
        }

        if (!referenceDataCache.isReady()) {
            throw new IllegalStateException("참조 데이터 캐시가 준비되지 않았습니다. Step 1이 완료되었는지 확인하세요.");
        }

        saveBooks(chunk.getItems());
    }

    /**
     * Book, BookImage, Batch 엔티티 저장
     * - 작가(BookAuthor)는 알라딘 API에서 처리하므로 여기서는 저장하지 않음
     */
    private void saveBooks(List<? extends BookNormalizedItem> items) {

        // 카테고리 조회
        Map<String, Category> categoryMap = persistenceHelper.loadCategories(items);
        if (categoryMap.isEmpty()) {
            log.debug("매칭되는 카테고리가 없어 저장을 건너뜁니다.");
            return;
        }

        // 중복 ISBN 필터링
        Set<String> existingIsbns = persistenceHelper.loadExistingIsbns(items);

        // Book 엔티티 생성
        List<Book> booksToSave = new ArrayList<>();
        List<BookNormalizedItem> validItems = new ArrayList<>();

        for (BookNormalizedItem item : items) {
            if (existingIsbns.contains(item.isbn13())) {
                continue;
            }

            Category category = categoryMap.get(item.kdcCode());
            Publisher publisher = referenceDataCache.findPublisher(item.publisherName());

            booksToSave.add(persistenceHelper.createBook(item, category, publisher));
            validItems.add(item);
        }

        if (booksToSave.isEmpty()) {
            log.debug("저장할 신규 도서가 없습니다.");
            return;
        }

        // DB 저장
        List<Book> savedBooks = persistenceHelper.saveBooks(booksToSave);
        
        Map<String, Book> savedBookMap = savedBooks.stream()
                .collect(Collectors.toMap(Book::getIsbn13, book -> book));

        // BookImage 저장
        int imageCount = persistenceHelper.saveBookImages(validItems, savedBookMap);

        // Batch 기록 저장
        persistenceHelper.saveBatchRecords(savedBooks);

        log.info("도서 저장 완료 - 신규: {}권, 이미지: {}건", savedBooks.size(), imageCount);
    }
}
