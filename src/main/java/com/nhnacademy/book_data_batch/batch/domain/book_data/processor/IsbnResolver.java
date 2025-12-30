package com.nhnacademy.book_data_batch.batch.domain.book_data.processor;

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

    public String resolve(String isbn13, String isbn10) {
        String trimmedIsbn13 = safeTrim(isbn13);
        String trimmedIsbn10 = safeTrim(isbn10);

        // 1. ISBN13이 있으면 그대로 사용
        if (StringUtils.hasText(trimmedIsbn13)) {
            return trimmedIsbn13;
        }

        // 2. ISBN10이 있으면 ISBN13으로 변환
        if (StringUtils.hasText(trimmedIsbn10)) {
            return convertIsbn10ToIsbn13(trimmedIsbn10);
        }

        // 둘 다 없으면 null
        return null;
    }

    private String convertIsbn10ToIsbn13(String isbn10) {

        if (isbn10 == null || isbn10.length() != 10) {
            return null;
        }

        // 1. "978" + ISBN-10의 앞 9자리
        String core = "978" + isbn10.substring(0, 9);
        
        // 2. 체크 숫자 계산
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Character.getNumericValue(core.charAt(i));
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        int remainder = sum % 10;
        int checkDigit = (remainder == 0) ? 0 : 10 - remainder;
        
        // 3. 최종 ISBN-13 반환
        return core + checkDigit;
    }

    private String safeTrim(String text) {
        return text == null ? null : text.trim();
    }
}
