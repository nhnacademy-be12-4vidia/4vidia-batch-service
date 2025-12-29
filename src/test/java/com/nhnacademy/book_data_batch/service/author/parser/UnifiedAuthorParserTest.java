package com.nhnacademy.book_data_batch.service.author.parser;

import com.nhnacademy.book_data_batch.service.author.parser.dto.ParsedAuthor;
import com.nhnacademy.book_data_batch.service.author.parser.strategy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UnifiedAuthorParser 테스트")
class UnifiedAuthorParserTest {

    private UnifiedAuthorParser parser;

    @BeforeEach
    void setUp() {
        parser = new UnifiedAuthorParser(List.of(
            new StrictKoreanNameStrategy(),
            new ParenthesizedRoleStrategy(),
            new BracketRoleStrategy(),
            new RoleSuffixStrategy()
        ));
    }

    @Test
    @DisplayName("null 입력 시 빈 리스트 반환")
    void parse_null_returnsEmptyList() {
        List<ParsedAuthor> result = parser.parse(null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("빈 문자열 입력 시 빈 리스트 반환")
    void parse_emptyString_returnsEmptyList() {
        List<ParsedAuthor> result = parser.parse("");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("세미콜론으로 구분된 다수 저자 파싱")
    void parse_semicolonSeparated_parsesEachPart() {
        List<ParsedAuthor> result = parser.parse("김철수(지은이); 이영희(옮긴이)");
        
        assertEquals(2, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
        assertEquals("이영희", result.get(1).name());
        assertEquals("옮긴이", result.get(1).role());
    }

    @Test
    @DisplayName("첫 번째 전략(ParenthesizedRole) 매칭")
    void parse_firstStrategyMatches_parenthesisFormat() {
        List<ParsedAuthor> result = parser.parse("김철수(지은이)");
        
        assertEquals(1, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
    }

    @Test
    @DisplayName("두 번째 전략(BracketRole) 매칭")
    void parse_secondStrategyMatches_bracketFormat() {
        List<ParsedAuthor> result = parser.parse("[지은이: 김철수, 옮긴이: 이영희]");
        
        assertEquals(2, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
        assertEquals("이영희", result.get(1).name());
        assertEquals("옮긴이", result.get(1).role());
    }

    @Test
    @DisplayName("세 번째 전략(RoleSuffix) 매칭")
    void parse_thirdStrategyMatches_roleSuffixFormat() {
        List<ParsedAuthor> result = parser.parse("김철수 지은이, 이영희 옮긴이");
        
        assertEquals(2, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
        assertEquals("이영희", result.get(1).name());
        assertEquals("옮긴이", result.get(1).role());
    }

    @Test
    @DisplayName("어떤 전략도 매칭하지 않을 때 fallback")
    void parse_noStrategyMatches_returnsDefault() {
        List<ParsedAuthor> result = parser.parse("John Doe");
        
        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).name());
        assertNull(result.get(0).role());
    }

    @Test
    @DisplayName("공백 처리")
    void parse_withSpaces_handlesWhitespace() {
        List<ParsedAuthor> result = parser.parse("  김철수(지은이)  ; 이영희(옮긴이)  ");
        
        assertEquals(2, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
        assertEquals("이영희", result.get(1).name());
        assertEquals("옮긴이", result.get(1).role());
    }
}
