package com.nhnacademy.book_data_batch.batch.domain.aladin.processor;

import com.nhnacademy.book_data_batch.batch.domain.aladin.dto.AladinFetchWrapper;
import com.nhnacademy.book_data_batch.batch.domain.aladin.dto.api.AladinItemDto;
import com.nhnacademy.book_data_batch.batch.domain.book_data.processor.IsbnResolver;
import com.nhnacademy.book_data_batch.domain.Book;
import com.nhnacademy.book_data_batch.domain.Category;
import com.nhnacademy.book_data_batch.infrastructure.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class AladinFetchProcessor implements ItemProcessor<AladinItemDto, AladinFetchWrapper> {

    private final CategoryRepository categoryRepository;
    private final IsbnResolver isbnResolver;

    @Override
    public AladinFetchWrapper process(AladinItemDto item) {
        // 1. ISBN 유효성 검사 및 정규화
        String isbn = isbnResolver.resolve(item.isbn13(), item.isbn());
        if (isbn == null) {
            log.debug("[AladinFetchProcessor] 유효하지 않은 ISBN - Title: {}", item.title());
            return null; // Skip
        }

        // 2. 카테고리 매핑 (KDC Code '005'인 카테고리 조회)
        Category category = categoryRepository.findByKdcCode("005")
                .orElseThrow(() -> new IllegalStateException("카테고리 '005'가 존재하지 않습니다."));

        // 4. 출판일 파싱
        LocalDate pubDate = parsePubDate(item.pubDate());

        // 5. Book 엔티티 생성
        Book book = Book.builder()
                .isbn(isbn)
                .title(item.title())
                .description(item.description()) // 요약 정보
                .publishedDate(pubDate)
                .priceStandard(item.priceStandard())
                .priceSales(item.priceStandard() * 9 / 10)
                .category(category) // 카테고리 설정
                .stock(100) // 기본 재고
                .build();

        return new AladinFetchWrapper(book, item.publisher());
    }

    private LocalDate parsePubDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {}", dateStr);
            return null;
        }
    }
}