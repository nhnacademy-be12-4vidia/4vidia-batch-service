package com.nhnacademy.book_data_batch.service.author.parser.strategy;

import com.nhnacademy.book_data_batch.service.author.parser.dto.ParsedAuthor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StrictKoreanNameStrategy 테스트")
class StrictKoreanNameStrategyTest {

    private final StrictKoreanNameStrategy strategy = new StrictKoreanNameStrategy();

    @Test
    @DisplayName("빈 문자열 입력 시 빈 리스트 반환")
    void parse_emptyString_returnsEmptyList() {
        List<ParsedAuthor> result = strategy.parse("");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("2글자 한국 이름 파싱")
    void parse_twoCharKoreanName_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("김철수");
        
        assertEquals(1, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
    }

    @Test
    @DisplayName("3글자 한국 이름 파싱")
    void parse_threeCharKoreanName_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("홍길동");
        
        assertEquals(1, result.size());
        assertEquals("홍길동", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
    }

    @Test
    @DisplayName("쉼표로 구분된 다수 이름 파싱")
    void parse_commaSeparatedNames_returnsMultipleParsed() {
        List<ParsedAuthor> result = strategy.parse("김철수,이영희");
        
        assertEquals(2, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
        assertEquals("이영희", result.get(1).name());
        assertEquals("지은이", result.get(1).role());
    }

    @Test
    @DisplayName("1글자 이름은 실패")
    void parse_oneCharName_returnsEmptyList() {
        List<ParsedAuthor> result = strategy.parse("김");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("4글자 이상 이름은 실패")
    void parse_fourOrMoreCharName_returnsEmptyList() {
        List<ParsedAuthor> result = strategy.parse("홍길동이");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("영문 이름은 실패")
    void parse_englishName_returnsEmptyList() {
        List<ParsedAuthor> result = strategy.parse("John Doe");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("혼합 문자(한글+영문)는 실패")
    void parse_mixedLanguageName_returnsEmptyList() {
        List<ParsedAuthor> result = strategy.parse("김철수John");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("공백 포함은 실패")
    void parse_nameWithSpaces_returnsEmptyList() {
        List<ParsedAuthor> result = strategy.parse(" 김철수 ");
        assertTrue(result.isEmpty());
    }
}
