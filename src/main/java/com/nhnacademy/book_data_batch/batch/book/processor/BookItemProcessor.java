package com.nhnacademy.book_data_batch.batch.book.processor;

import com.nhnacademy.book_data_batch.batch.book.dto.BookCsvRow;
import com.nhnacademy.book_data_batch.batch.book.dto.BookNormalizedItem;
import com.nhnacademy.book_data_batch.batch.book.formatter.DateFormatter;
import com.nhnacademy.book_data_batch.batch.book.resolver.AuthorRoleResolver;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.nhnacademy.book_data_batch.batch.book.resolver.IsbnResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.util.StringUtils;

// Processor: BookCsvRow -> BookNormalizedItem 변환 (정규화 및 유효성 검사 포함)
@Slf4j
@RequiredArgsConstructor
public class BookItemProcessor implements ItemProcessor<BookCsvRow, BookNormalizedItem> {

    private final IsbnResolver isbnResolver;
    private final AuthorRoleResolver authorRoleResolver;

    @Override
    public BookNormalizedItem process(BookCsvRow item) {

        // ISBN 처리 (NOT NULL)
        String isbn = isbnResolver.resolve(item.isbn13(), item.isbn10());
        if (!StringUtils.hasText(isbn)) {
            log.warn("ISBN이 없어 레코드를 건너뜁니다. isbn13={}, isbn10={}", item.isbn13(), item.isbn10());
            return null;
        }

        // 제목 처리 (NOT NULL)
        String title = item.title() != null ? item.title().trim() : null;
        if (!StringUtils.hasText(item.title())) {
            log.warn("제목이 없어 레코드를 건너뜁니다. title={}", title);
            return null;
        }

        // 출판사 처리 (NULLABLE)
        String publisher = safeTrim(item.publisher());
        if (!StringUtils.hasText(publisher)) {
            publisher = "출판사 미상";
        }

        // 정규화된 도서 정보 생성
        return BookNormalizedItem.builder()
            // 책 기본 정보
            .isbn13(isbn)
            .title(title)
            .description(blankToNull(item.description()))
            .publishedDate(parsePublishedDate(item.publishedDate(), item.secondaryPublishedDate()))
            .priceStandard(parsePrice(item.price()))
            .volumeNumber(parseVolumeNumber(item.volumeNumber()))
            // 이미지, 카테고리, 출판사, 작가
            .imageUrl(blankToNull(item.imageUrl()))
            .kdcCode(normalizeKdc(item.kdcCode()))
            .publisherName(publisher)
            .authorRoles(authorRoleResolver.parse(item.author()))
            .build();
    }

    // 가격 String -> Integer 변환
    private Integer parsePrice(String priceText) {
        if (!StringUtils.hasText(priceText)) {
            return null;
        }
        try {
            return Integer.parseInt(priceText);
        } catch (NumberFormatException ex) {
            log.debug("가격 파싱 실패로 0원 처리. price={}, message={} ", priceText, ex.getMessage());
            return 0;
        }
    }

    // 출판일 String -> LocalDate 변환
    private LocalDate parsePublishedDate(String primary, String secondary) {
        String value = StringUtils.hasText(primary) ? primary : secondary;
        if (!StringUtils.hasText(value)) {
            return null;
        }
        value = value.trim();
        for (DateTimeFormatter formatter : DateFormatter.PATTERNS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (Exception ignored) {
                // 포맷이 맞지 않으면 다음 후보로 넘어간다.
            }
        }
        log.debug("날짜 포매팅 실패로 null 반환 value={}", value);
        return null;
    }

    // 권수 String -> Integer 변환
    private Integer parseVolumeNumber(String volumeText) {
        if (!StringUtils.hasText(volumeText)) {
            return 1;
        }
        try {
            return Integer.parseInt(volumeText.trim());
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    // KDC 코드 정규화 (세목 버리기: 앞부분 숫자만 추출)
    private String normalizeKdc(String rawKdc) {
        if (!StringUtils.hasText(rawKdc)) {
            return null;
        }

        // 첫 번째 점(.) 이전 부분 추출
        String trimmed = rawKdc.trim();
        String[] tokens = trimmed.split("\\.");
        String base = tokens[0];
        if (!StringUtils.hasText(base)) {
            return null;
        }

        // 숫자 이외 문자 제거
        base = base.replaceAll("\\D", "");
        if (!StringUtils.hasText(base)) {
            return null;
        }

        return base;
    }

    private String safeTrim(String text) {
        return text == null ? null : text.trim();
    }

    private String blankToNull(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }
}
