package com.nhnacademy.book_data_batch.batch.book.resolver;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * ISBN을 처리하는 유틸리티 클래스입니다.
 * - ISBN13이 있다면 ISBN13을 반환합니다.
 * - ISBN13이 없는데, ISBN10이 있다면 ISBN13으로 변환해 반환합니다.
 * - 둘 다 없다면, null을 반환합니다.
 */
@Component
public class IsbnResolver {

    public String resolve(String isbn13, String isbn10) {
        String trimmedIsbn13 = safeTrim(isbn13);
        String trimmedIsbn10 = safeTrim(isbn10);

        if (StringUtils.hasText(trimmedIsbn13)) {
            return trimmedIsbn13;
        }

        if (StringUtils.hasText(trimmedIsbn10)) {
            return convertIsbn10ToIsbn13(trimmedIsbn10);
        }

        return null;
    }

    private String convertIsbn10ToIsbn13(String isbn10) {

        if (isbn10 == null || isbn10.length() != 10) {
            return null;
        }

        String core = "978" + isbn10.substring(0, 9);
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Character.getNumericValue(core.charAt(i));
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        int remainder = sum % 10;
        int checkDigit = (remainder == 0) ? 0 : 10 - remainder;
        return core + checkDigit;
    }

    private String safeTrim(String text) {
        return text == null ? null : text.trim();
    }
}
