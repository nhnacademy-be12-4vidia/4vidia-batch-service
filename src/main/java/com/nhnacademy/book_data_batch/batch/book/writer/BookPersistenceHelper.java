package com.nhnacademy.book_data_batch.batch.book.writer;

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
import java.util.stream.Collectors;

/**
 * 도서 데이터 영속화 헬퍼
 * - Book 및 연관 엔티티(BookImage, Batch)의 DB 저장 담당
 * - 작가(BookAuthor)는 알라딘 API에서 처리하므로 여기서는 제외
 *
 * @see BookItemWriter Writer에서 이 헬퍼를 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookPersistenceHelper {
    
    private final BookRepository bookRepository;
    private final BookImageRepository bookImageRepository;
    private final CategoryRepository categoryRepository;
    private final BatchRepository batchRepository;

    // Read

    /**
     * KDC 코드로 카테고리 맵 조회
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
     * Book 엔티티 생성 (rawAuthor 포함)
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
                .rawAuthor(item.rawAuthor())  // CSV 원본 작가 필드 저장
                .build();
    }

    // Persist (Save)

    /**
     * 도서 목록 일괄 저장
     */
    public List<Book> saveBooks(List<Book> books) {
        if (books.isEmpty()) {
            return Collections.emptyList();
        }
        return bookRepository.saveAll(books);
    }

    /**
     * 도서 이미지 저장
     */
    public int saveBookImages(List<BookNormalizedItem> items, Map<String, Book> bookMap) {
        List<BookImageDto> images = buildBookImageDtos(items, bookMap);
        
        if (images.isEmpty()) {
            return 0;
        }

        bookImageRepository.bulkInsert(images);
        return images.size();
    }

    /**
     * Batch 기록 저장
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
