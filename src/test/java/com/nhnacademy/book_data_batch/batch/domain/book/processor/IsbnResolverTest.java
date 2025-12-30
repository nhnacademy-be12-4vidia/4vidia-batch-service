package com.nhnacademy.book_data_batch.batch.domain.book.processor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IsbnResolver 테스트")
class IsbnResolverTest {

    @InjectMocks
    private IsbnResolver isbnResolver;

    @Test
    @DisplayName("올바른 ISBN13은 그대로 반환")
    void resolve_validIsbn13_returnsSame() {
        String isbn13 = "9780123456789";
        String result = isbnResolver.resolve(isbn13, null);
        assertEquals(isbn13, result);
    }

    @Test
    @DisplayName("올바른 ISBN10을 ISBN13으로 변환")
    void resolve_validIsbn10_convertsToIsbn13() {
        String result = isbnResolver.resolve(null, "0123456789");
        assertEquals("9780123456786", result);
    }

    @Test
    @DisplayName("ISBN10과 ISBN13 모두 null 시 null 반환")
    void resolve_bothNull_returnsNull() {
        String result = isbnResolver.resolve(null, null);
        assertNull(result);
    }

    @Test
    @DisplayName("잘못된 ISBN10은 null 반환")
    void resolve_invalidIsbn10_returnsNull() {
        String result = isbnResolver.resolve(null, "123456789");
        assertNull(result);
    }

    @Test
    @DisplayName("ISBN13이 있으면 ISBN10은 무시")
    void resolve_isbn13Present_ignoresIsbn10() {
        String result = isbnResolver.resolve("9780123456789", "0123456789");
        assertEquals("9780123456789", result);
    }

    @Test
    @DisplayName("공백 ISBN13은 trim 처리")
    void resolve_whitespaceIsbn13_trims() {
        String result = isbnResolver.resolve("  9780123456789  ", null);
        assertEquals("9780123456789", result);
    }

    @Test
    @DisplayName("공백 ISBN10은 trim 처리")
    void resolve_whitespaceIsbn10_trims() {
        String result = isbnResolver.resolve(null, "  0123456789  ");
        assertEquals("9780123456786", result);
    }

    @Test
    @DisplayName("ISBN10 'X' 체크숫자 변환")
    void resolve_validIsbn10WithX_convertsToIsbn13() {
        String result = isbnResolver.resolve(null, "055337944X");
        assertEquals("9780553379440", result);
    }
}
