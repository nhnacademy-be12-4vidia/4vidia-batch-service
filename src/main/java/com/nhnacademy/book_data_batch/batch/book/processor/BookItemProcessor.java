package com.nhnacademy.book_data_batch.batch.book.processor;

import com.nhnacademy.book_data_batch.batch.book.dto.BookCsvRow;
import com.nhnacademy.book_data_batch.batch.book.dto.BookNormalizedItem;
import com.nhnacademy.book_data_batch.service.AuthorNameExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * <pre>
 * BookItemProcessor
 * - CSV 원시 데이터(BookCsvRow) → 정규화된 도서 정보(BookNormalizedItem)
 * 
 * @see BookCsvRow Reader에서 전달받는 원시 데이터 DTO
 * @see BookNormalizedItem Writer에 전달할 정규화된 DTO
 * @see FieldNormalizer 필드 변환 유틸리티
 * @see IsbnResolver ISBN 처리 유틸리티
 * @see AuthorNameExtractor 저자명 추출 서비스
 * </pre>
 */
@Slf4j
@RequiredArgsConstructor
public class BookItemProcessor implements ItemProcessor<BookCsvRow, BookNormalizedItem> {

    private final IsbnResolver isbnResolver;
    private final AuthorNameExtractor authorNameExtractor;
    private final FieldNormalizer fieldNormalizer;

    /**
     * process
     * 1. ISBN 유효성 검사 - 필수, 없으면 null 반환
     * 2. 제목 유효성 검사 - 필수, 없으면 null 반환
     * 3. 저자 이름 추출 - 복합 저자 문자열 분리
     * 4. 필드 정규화 - 날짜, 가격, KDC 등 변환
     * 5. BookNormalizedItem 생성
     * 
     * @param item CSV에서 읽은 원시 데이터
     * @return 정규화된 도서 정보, 유효하지 않으면 null
     */
    @Override
    public BookNormalizedItem process(BookCsvRow item) {
        // 1. ISBN 유효성 검사 (필수)
        String isbn = isbnResolver.resolve(item.isbn13(), item.isbn10());
        if (!StringUtils.hasText(isbn)) {
            log.debug("ISBN이 없어 레코드를 건너뜁니다. isbn13={}, isbn10={}", item.isbn13(), item.isbn10());
            return null;
        }

        // 2. 제목 유효성 검사 (필수)
        String title = fieldNormalizer.trimOrNull(item.title());
        if (!StringUtils.hasText(title)) {
            log.debug("제목이 없어 레코드를 건너뜁니다. isbn={}", isbn);
            return null;
        }

        // 3. 저자 이름 추출
        List<String> authorNames = authorNameExtractor.extractAuthorNames(item.author());

        // 4. 정규화된 DTO 생성
        return new BookNormalizedItem(
                isbn,
                title,
                fieldNormalizer.blankToNull(item.description()),
                fieldNormalizer.parseDate(item.publishedDate(), item.secondaryPublishedDate()),
                fieldNormalizer.parsePrice(item.price()),
                fieldNormalizer.parseVolumeNumber(item.volumeNumber()),
                fieldNormalizer.blankToNull(item.imageUrl()),
                fieldNormalizer.normalizeKdc(item.kdcCode()),
                fieldNormalizer.defaultIfBlank(item.publisher(), "출판사 미상"),
                authorNames,
                item.author() // TODO: 추후 작가 역할 파싱용 원본 필드 보존
        );
    }
}
