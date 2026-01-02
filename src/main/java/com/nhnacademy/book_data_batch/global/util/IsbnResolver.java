package com.nhnacademy.book_data_batch.global.util;

import org.apache.commons.validator.routines.ISBNValidator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * <pre>
 * ISBN Resolver
 * - ISBN13과 ISBN10 중 유효한 ISBN을 결정
 * - ISBN10을 ISBN13으로 변환
 * - 모든 도서 식별자를 ISBN13으로 통일
 * </pre>
 */
@Component
public class IsbnResolver {

    private final ISBNValidator isbnValidator = ISBNValidator.getInstance(true);

    public String resolve(String isbn13, String isbn10) {
        String normalizedIsbn13 = isbnValidator.validateISBN13(normalizeIsbn(isbn13));
        String normalizedIsbn10 = isbnValidator.validateISBN10(normalizeIsbn(isbn10));

        // 1. ISBN13이 있으면 그대로 사용
        if (StringUtils.hasText(normalizedIsbn13)) {
            return normalizedIsbn13;
        }

        // 2. ISBN10이 있으면 ISBN13으로 변환
        if (StringUtils.hasText(normalizedIsbn10)) {
            return isbnValidator.convertToISBN13(normalizedIsbn10);
        }

        // 둘 다 없으면 null
        return null;
    }

    private String normalizeIsbn(String text) {
        return text == null ? null : text.trim().replace("-", "");
    }
}
