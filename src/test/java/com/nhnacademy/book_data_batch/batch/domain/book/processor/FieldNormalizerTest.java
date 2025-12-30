package com.nhnacademy.book_data_batch.batch.domain.book.processor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FieldNormalizer 테스트")
class FieldNormalizerTest {

    @InjectMocks
    private FieldNormalizer fieldNormalizer;

    @Test
    @DisplayName("null 값 trimOrNull 시 null 반환")
    void trimOrNull_null_returnsNull() {
        assertNull(fieldNormalizer.trimOrNull(null));
    }

    @Test
    @DisplayName("빈 문자열 trimOrNull 시 null 반환")
    void trimOrNull_emptyString_returnsNull() {
        assertNull(fieldNormalizer.trimOrNull(""));
        assertNull(fieldNormalizer.trimOrNull("   "));
    }

    @Test
    @DisplayName("문자열 trimOrNull 시 trim된 문자열 반환")
    void trimOrNull_text_returnsTrimmedText() {
        assertEquals("hello", fieldNormalizer.trimOrNull("  hello  "));
    }

    @Test
    @DisplayName("null 값 blankToNull 시 null 반환")
    void blankToNull_null_returnsNull() {
        assertNull(fieldNormalizer.blankToNull(null));
    }

    @Test
    @DisplayName("빈 문자열 blankToNull 시 null 반환")
    void blankToNull_emptyString_returnsNull() {
        assertNull(fieldNormalizer.blankToNull(""));
        assertNull(fieldNormalizer.blankToNull("   "));
    }

    @Test
    @DisplayName("문자열 blankToNull 시 원본 반환")
    void blankToNull_text_returnsOriginalText() {
        assertEquals("hello", fieldNormalizer.blankToNull("hello"));
    }

    @Test
    @DisplayName("빈 문자열 defaultIfBlank 시 기본값 반환")
    void defaultIfBlank_empty_returnsDefault() {
        assertEquals("default", fieldNormalizer.defaultIfBlank("", "default"));
        assertEquals("default", fieldNormalizer.defaultIfBlank("   ", "default"));
    }

    @Test
    @DisplayName("문자열 defaultIfBlank 시 원본 반환")
    void defaultIfBlank_text_returnsOriginalText() {
        assertEquals("hello", fieldNormalizer.defaultIfBlank("hello", "default"));
    }

    @Test
    @DisplayName("null 값 defaultIfBlank 시 기본값 반환")
    void defaultIfBlank_null_returnsDefault() {
        assertEquals("default", fieldNormalizer.defaultIfBlank(null, "default"));
    }

    @Test
    @DisplayName("올바른 가격 포맷 parsePrice 시 숫자 반환")
    void parsePrice_validFormat_returnsNumber() {
        assertEquals(10000, fieldNormalizer.parsePrice("10,000"));
        assertEquals(1234, fieldNormalizer.parsePrice("1,234"));
        assertEquals(999999, fieldNormalizer.parsePrice("999,999"));
        assertEquals(0, fieldNormalizer.parsePrice("0"));
    }

    @Test
    @DisplayName("잘못된 가격 포맷 parsePrice 시 0 반환")
    void parsePrice_invalidFormat_returnsZero() {
        assertEquals(0, fieldNormalizer.parsePrice("invalid"));
        assertEquals(0, fieldNormalizer.parsePrice("abc"));
    }

    @Test
    @DisplayName("null 값 parsePrice 시 null 반환")
    void parsePrice_null_returnsNull() {
        assertNull(fieldNormalizer.parsePrice(null));
    }

    @Test
    @DisplayName("빈 문자열 parsePrice 시 null 반환")
    void parsePrice_emptyString_returnsNull() {
        assertNull(fieldNormalizer.parsePrice(""));
        assertNull(fieldNormalizer.parsePrice("   "));
    }

    @Test
    @DisplayName("올바른 yyyy-MM-dd 포맷 parseDate 시 LocalDate 반환")
    void parseDate_yyyyMMdd_returnsLocalDate() {
        LocalDate result = fieldNormalizer.parseDate("2024-01-15", null);
        assertEquals(LocalDate.of(2024, 1, 15), result);
    }

    @Test
    @DisplayName("올바른 yyyyMMdd 포맷 parseDate 시 LocalDate 반환")
    void parseDate_yyyyMMddFormat_returnsLocalDate() {
        LocalDate result = fieldNormalizer.parseDate(null, "20240512");
        assertEquals(LocalDate.of(2024, 5, 12), result);
    }

    @Test
    @DisplayName("잘못된 날짜 포맷 parseDate 시 null 반환")
    void parseDate_invalidFormat_returnsNull() {
        assertNull(fieldNormalizer.parseDate("invalid", null));
        assertNull(fieldNormalizer.parseDate(null, "abc"));
    }

    @Test
    @DisplayName("null 값 parseDate 시 null 반환")
    void parseDate_null_returnsNull() {
        assertNull(fieldNormalizer.parseDate(null, null));
    }

    @Test
    @DisplayName("올바른 권 번호 parseVolumeNumber 시 숫자 반환")
    void parseVolumeNumber_valid_returnsNumber() {
        assertEquals(3, fieldNormalizer.parseVolumeNumber("3"));
        assertEquals(1, fieldNormalizer.parseVolumeNumber("1"));
        assertEquals(10, fieldNormalizer.parseVolumeNumber("10"));
    }

    @Test
    @DisplayName("빈 문자열/잘못된 값 parseVolumeNumber 시 기본값 1 반환")
    void parseVolumeNumber_invalidOrEmpty_returnsDefault() {
        assertEquals(1, fieldNormalizer.parseVolumeNumber(""));
        assertEquals(1, fieldNormalizer.parseVolumeNumber("   "));
        assertEquals(1, fieldNormalizer.parseVolumeNumber("invalid"));
        assertEquals(1, fieldNormalizer.parseVolumeNumber("abc"));
    }

    @Test
    @DisplayName("null 값 parseVolumeNumber 시 기본값 1 반환")
    void parseVolumeNumber_null_returnsDefault() {
        assertEquals(1, fieldNormalizer.parseVolumeNumber(null));
    }

    @Test
    @DisplayName("올바른 KDC 코드 normalizeKdc 시 정제된 코드 반환")
    void normalizeKdc_valid_returnsCleanCode() {
        assertEquals("813", fieldNormalizer.normalizeKdc("813.7"));
        assertEquals("813", fieldNormalizer.normalizeKdc("8-13"));
        assertEquals("005", fieldNormalizer.normalizeKdc("005"));
        assertEquals("123", fieldNormalizer.normalizeKdc("123"));
    }

    @Test
    @DisplayName("빈 문자열 또는 비숫자 normalizeKdc 시 UNC 반환")
    void normalizeKdc_invalid_returnsUNC() {
        assertEquals("UNC", fieldNormalizer.normalizeKdc(null));
        assertEquals("UNC", fieldNormalizer.normalizeKdc(""));
        assertEquals("UNC", fieldNormalizer.normalizeKdc("ABC"));
    }
}
