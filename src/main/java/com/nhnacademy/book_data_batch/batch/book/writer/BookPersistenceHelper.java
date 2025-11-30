package com.nhnacademy.book_data_batch.batch.book.writer;

import com.nhnacademy.book_data_batch.batch.book.cache.ReferenceDataCache;
import com.nhnacademy.book_data_batch.batch.book.dto.BookAuthorDto;
import com.nhnacademy.book_data_batch.batch.book.dto.BookImageDto;
import com.nhnacademy.book_data_batch.batch.book.dto.BookNormalizedItem;
import com.nhnacademy.book_data_batch.entity.*;
import com.nhnacademy.book_data_batch.entity.enums.ImageType;
import com.nhnacademy.book_data_batch.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 도서 데이터 영속화 헬퍼
 * - Book 및 연관 엔티티(BookAuthor, BookImage, Batch)의 DB 저장 담당
 *
 * 동기화 전략
 * - BookAuthor INSERT는 여러 파티션이 동시 실행 시 Deadlock 발생 가능
 * -> BOOK_AUTHOR_LOCK으로 동기화해 순차 처리
 *
 * @see BookItemWriter Writer에서 이 헬퍼를 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookPersistenceHelper {

    private static final String DEFAULT_AUTHOR_ROLE = "지은이"; // 일단 이걸로..

    // 전역 락: BookAuthor INSERT 동기화용
    // TODO: 왜 ReentrantLock이냐면 synchronized보다 성능이 더 좋고,
    // 명시적으로 락 획득/해제를 제어할 수 있어서 예외 상황 처리에 유리함??
    private static final ReentrantLock BOOK_AUTHOR_LOCK = new ReentrantLock();
    
    private final BookRepository bookRepository;
    private final BookAuthorRepository bookAuthorRepository;
    private final BookImageRepository bookImageRepository;
    private final CategoryRepository categoryRepository;
    private final BatchRepository batchRepository;

    // Read

    /**
     * KDC 코드로 카테고리 맵 조회
     *
     * @param items 처리할 도서 아이템 목록
     * @return KDC 코드 → Category 엔티티 매핑
     */
    public Map<String, Category> loadCategories(List<? extends BookNormalizedItem> items) {
        Set<String> codes = items.stream()
                .map(BookNormalizedItem::kdcCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        if (codes.isEmpty()) {
            return Collections.emptyMap();
        }

        return categoryRepository.findAllByKdcCodeIn(codes).stream()
                .collect(Collectors.toMap(Category::getKdcCode, c -> c));
    }

    /**
     * 이미 DB에 존재하는 ISBN 목록 조회
     *
     * @param items 처리할 도서 아이템 목록
     * @return 이미 존재하는 ISBN 집합
     */
    public Set<String> loadExistingIsbns(List<? extends BookNormalizedItem> items) {
        Set<String> candidates = items.stream()
                .map(BookNormalizedItem::isbn13)
                .collect(Collectors.toSet());

        if (candidates.isEmpty()) {
            return Collections.emptySet();
        }

        return bookRepository.findAllByIsbn13In(candidates).stream()
                .map(Book::getIsbn13)
                .collect(Collectors.toSet());
    }

    // Create

    /**
     * Book 엔티티 생성
     *
     * @param item      정규화된 도서 데이터
     * @param category  카테고리 엔티티
     * @param publisher 출판사 엔티티
     * @return 생성된 Book 엔티티
     */
    public Book createBook(BookNormalizedItem item, Category category, Publisher publisher) {
        int priceStandard = item.priceStandard() != null ? item.priceStandard() : 0;
        int priceSales = priceStandard * 9 / 10;
        
        return Book.builder()
                .isbn13(item.isbn13())
                .title(item.title())
                .description(item.description())
                .publisher(publisher)
                .publishedDate(item.publishedDate())
                .priceStandard(priceStandard)
                .priceSales(priceSales)
                .category(category)
                .volumeNumber(item.volumeNumber())
                .build();
    }

    // Persist (Save)

    /**
     * 도서 목록 일괄 저장합니다.
     *
     * @param books 저장할 Book 엔티티 목록
     * @return 저장된 Book 엔티티 목록 (ID가 할당됨)
     */
    public List<Book> saveBooks(List<Book> books) {
        if (books.isEmpty()) {
            return Collections.emptyList();
        }
        // 저장 후에 book_id가 필요해서 jpa saveAll 사용
        return bookRepository.saveAll(books);
    }

    /**
     * Book-Author 연관관계 저장
     * - Deadlock 방지를 위해 전역 락으로 동기화
     *
     * @param items   원본 도서 아이템 목록 (작가 정보 포함)
     * @param bookMap ISBN → 저장된 Book 엔티티 매핑
     * @param cache   참조 데이터 캐시 (Author 조회용)
     * @return 저장된 연관관계 수
     */
    public int saveBookAuthors(List<BookNormalizedItem> items, Map<String, Book> bookMap, ReferenceDataCache cache) {
        List<BookAuthorDto> relations = buildBookAuthorDtos(items, bookMap, cache);
        
        if (relations.isEmpty()) {
            return 0;
        }

        BOOK_AUTHOR_LOCK.lock();
        try {
            bookAuthorRepository.bulkInsert(relations);
        } finally {
            BOOK_AUTHOR_LOCK.unlock();
        }
        
        return relations.size();
    }

    /**
     * 도서 이미지 저장
     *
     * @param items   원본 도서 아이템 목록 (이미지 URL 포함)
     * @param bookMap ISBN → 저장된 Book 엔티티 매핑
     * @return 저장된 이미지 수
     */
    public int saveBookImages(List<BookNormalizedItem> items, Map<String, Book> bookMap) {
        List<BookImageDto> images = buildBookImageDtos(items, bookMap);
        
        if (images.isEmpty()) {
            return 0;
        }

        bookImageRepository.bulkInsert(images); // TODO: 이건 락을 안 걸어도 deadlock이 안 걸리네??
        // 아마 같은 book_id에 대해 동시에 insert하는 경우가 없어서 그런 듯?
        return images.size();
    }

    /**
     * Batch 기록 저장
     *
     * @param savedBooks 저장된 Book 엔티티 목록
     */
    public void saveBatchRecords(List<Book> savedBooks) {
        if (savedBooks.isEmpty()) {
            return;
        }

        List<Batch> batches = savedBooks.stream()
                .map(Batch::new)
                .toList();
        
        batchRepository.saveAll(batches);
    }

    // Private 헬퍼 메서드
    /**
     * BookAuthor DTO List 생성
     */
    private List<BookAuthorDto> buildBookAuthorDtos(
            List<BookNormalizedItem> items, 
            Map<String, Book> bookMap, 
            ReferenceDataCache cache
    ) {
        List<BookAuthorDto> relations = new ArrayList<>();
        Set<String> addedPairs = new HashSet<>();

        for (BookNormalizedItem item : items) {
            Book book = bookMap.get(item.isbn13());
            if (book == null || book.getId() == null) {
                continue;
            }
            
            if (item.authorNames() == null || item.authorNames().isEmpty()) {
                continue;
            }

            for (String authorName : item.authorNames()) {
                Author author = cache.findAuthor(authorName);
                if (author == null || author.getId() == null) {
                    log.trace("작가를 찾을 수 없음: {}", authorName);
                    continue;
                }

                String pairKey = book.getId() + "-" + author.getId();
                if (addedPairs.add(pairKey)) {
                    relations.add(new BookAuthorDto(
                            book.getId(),
                            author.getId(),
                            DEFAULT_AUTHOR_ROLE
                    ));
                }
            }
        }

        return relations;
    }

    /**
     * BookImage DTO List 생성
     */
    private List<BookImageDto> buildBookImageDtos(
            List<BookNormalizedItem> items, 
            Map<String, Book> bookMap
    ) {
        List<BookImageDto> images = new ArrayList<>();

        for (BookNormalizedItem item : items) {
            if (!StringUtils.hasText(item.imageUrl())) {
                continue;
            }

            Book book = bookMap.get(item.isbn13());
            if (book == null || book.getId() == null) {
                continue;
            }

            images.add(new BookImageDto(
                    book.getId(),
                    item.imageUrl(),
                    ImageType.THUMBNAIL.getCode()
            ));
        }

        return images;
    }
}
