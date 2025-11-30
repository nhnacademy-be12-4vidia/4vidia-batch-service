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
 * 0. 청크 단위로 BookNormalizedItem 리스트 수신
 * 1. ReferenceDataCache에서 Author/Publisher 조회
 * 2. Book, BookAuthor, BookImage, Batch 엔티티 저장
 * 
 * @see ReferenceDataLoadTasklet Step 1에서 참조 데이터 사전 로딩
 * @see BookPersistenceHelper 도서 영속화 담당
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional
public class BookItemWriter implements ItemWriter<BookNormalizedItem> {

    private final BookPersistenceHelper persistenceHelper; // DB 저장 위임
    private final ReferenceDataCache referenceDataCache;   // 작가/출판사 캐시

    /**
     * 청크 데이터 -> DB 저장
     * 
     * @param chunk 처리할 도서 데이터 청크
     * @throws Exception 저장 중 오류 발생 시
     */
    @Override
    public void write(Chunk<? extends BookNormalizedItem> chunk) throws Exception {

        // 청크가 비어있으면 처리 건너뜀
        if (chunk.isEmpty()) {
            return;
        }

        // Step 1에서 캐시가 구축되지 않았으면 예외 발생
        if (!referenceDataCache.isReady()) {
            throw new IllegalStateException("참조 데이터 캐시가 준비되지 않았습니다. Step 1이 완료되었는지 확인하세요.");
        }

        // 캐시에서 작가/출판사 조회 후 Book 저장
        saveBooks(chunk.getItems());
    }

    /**
     * Book, BookAuthor, BookImage, Batch 엔티티 저장
     * 1. 카테고리 조회 - KDC 코드로 Category 엔티티 조회
     * 2. 중복 체크 - 이미 저장된 ISBN 필터링
     * 3. 캐시에서 Publisher 조회
     * 4. Book 엔티티 생성 및 저장
     * 5. BookAuthor 연관관계 저장
     * 6. Batch 기록 저장
     * 
     * @param items 처리할 도서 데이터
     */
    private void saveBooks(List<? extends BookNormalizedItem> items) {

        // 카테고리 조회: KDC 코드 → Category 엔티티 매핑
        Map<String, Category> categoryMap = persistenceHelper.loadCategories(items);
        if (categoryMap.isEmpty()) {
            log.debug("매칭되는 카테고리가 없어 저장을 건너뜁니다.");
            return;
        }

        // 중복 ISBN 필터링: 이미 DB에 존재하는 ISBN은 저장하지 않음
        Set<String> existingIsbns = persistenceHelper.loadExistingIsbns(items);

        // Book 엔티티 생성
        List<Book> booksToSave = new ArrayList<>();
        List<BookNormalizedItem> validItems = new ArrayList<>();  // 실제 저장할 아이템 추적

        for (BookNormalizedItem item : items) {
            if (existingIsbns.contains(item.isbn13())) {
                continue;
            }

            // 카테고리 조회
            Category category = categoryMap.get(item.kdcCode());
            
            // 출판사 조회
            Publisher publisher = referenceDataCache.findPublisher(item.publisherName());

            // Book 엔티티 생성
            booksToSave.add(persistenceHelper.createBook(item, category, publisher));
            validItems.add(item);
        }

        if (booksToSave.isEmpty()) {
            log.debug("저장할 신규 도서가 없습니다.");
            return;
        }

        // DB 저장
        // 1. Book 저장
        List<Book> savedBooks = persistenceHelper.saveBooks(booksToSave);
        
        // ISBN → Book 매핑
        Map<String, Book> savedBookMap = savedBooks.stream()
                .collect(Collectors.toMap(Book::getIsbn13, book -> book));

        // 2. BookAuthor 저장
        int authorRelations = persistenceHelper.saveBookAuthors(validItems, savedBookMap, referenceDataCache);

        // 3. BookImage 저장
        int imageCount = persistenceHelper.saveBookImages(validItems, savedBookMap);

        // 4. Batch 기록 저장
        persistenceHelper.saveBatchRecords(savedBooks);

        log.info("도서 저장 완료 - 신규: {}권, 작가연결: {}건, 이미지: {}건", 
                savedBooks.size(), authorRelations, imageCount);
    }
}
