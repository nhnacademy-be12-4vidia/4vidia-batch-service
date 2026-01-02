package com.nhnacademy.book_data_batch.global.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("IsbnResolver 테스트")
class IsbnResolverTest {

    private IsbnResolver isbnResolver;

    @BeforeEach
    void setUp() {
        isbnResolver = new IsbnResolver();
    }

    @Test
    @DisplayName("올바른 ISBN13은 그대로 반환")
    void resolve_validIsbn13_returnsSame() {
        // 유효한 ISBN-13 예시
        String isbn13 = "9780321356680"; 
        String result = isbnResolver.resolve(isbn13, null);
        assertEquals(isbn13, result);
    }

    @Test
    @DisplayName("올바른 ISBN10을 ISBN13으로 변환")
    void resolve_validIsbn10_convertsToIsbn13() {
        // 유효한 ISBN-10 예시 (Effective Java)
        // 0321356683 -> 9780321356680
        String result = isbnResolver.resolve(null, "0321356683");
        assertEquals("9780321356680", result);
    }

    @Test
    @DisplayName("ISBN10과 ISBN13 모두 null 시 null 반환")
    void resolve_bothNull_returnsNull() {
        String result = isbnResolver.resolve(null, null);
        assertNull(result);
    }

    @Test
    @DisplayName("유효하지 않은 ISBN10은 null 반환")
    void resolve_invalidIsbn10_returnsNull() {
        // 체크 디지트가 틀린 ISBN
        String result = isbnResolver.resolve(null, "1234567890");
        assertNull(result);
    }

    @Test
    @DisplayName("ISBN13이 있으면 ISBN10은 무시")
    void resolve_isbn13Present_ignoresIsbn10() {
        String isbn13 = "9780321356680";
        String isbn10 = "0321356683";
        String result = isbnResolver.resolve(isbn13, isbn10);
        assertEquals(isbn13, result);
    }

    @Test
    @DisplayName("공백 ISBN13은 trim 및 하이픈 제거 처리")
    void resolve_whitespaceIsbn13_trims() {
        String result = isbnResolver.resolve("  978-0321356680  ", null);
        assertEquals("9780321356680", result);
    }

    @Test
    @DisplayName("공백 ISBN10은 trim 및 하이픈 제거 후 변환")
    void resolve_whitespaceIsbn10_trims() {
        String result = isbnResolver.resolve(null, "  0-321-35668-3  ");
        assertEquals("9780321356680", result);
    }

    @Test
    @DisplayName("ISBN10 'X' 체크숫자 변환")
    void resolve_validIsbn10WithX_convertsToIsbn13() {
        // 유효한 ISBN-10 (X 포함) -> ISBN-13
        // 080442957X -> 9780804429573
        String result = isbnResolver.resolve(null, "080442957X");
        assertEquals("9780804429573", result);
    }
}
