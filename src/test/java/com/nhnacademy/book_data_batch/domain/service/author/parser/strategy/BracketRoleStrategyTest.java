package com.nhnacademy.book_data_batch.domain.service.author.parser.strategy;

import com.nhnacademy.book_data_batch.domain.service.author.parser.dto.ParsedAuthor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BracketRoleStrategy 테스트")
class BracketRoleStrategyTest {

    private final BracketRoleStrategy strategy = new BracketRoleStrategy();

    @Test
    @DisplayName("빈 문자열 입력 시 빈 리스트 반환")
    void parse_emptyString_returnsEmptyList() {
        List<ParsedAuthor> result = strategy.parse("");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("기본 대괄호 형식 파싱: [지은이: 이름]")
    void parse_basicBracketFormat_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("[지은이: 김철수]");

        assertEquals(1, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
    }

    @Test
    @DisplayName("대괄호 없이 역할: 이름 형식 파싱")
    void parse_roleColonNameFormat_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("지은이: 홍길동");

        assertEquals(1, result.size());
        assertEquals("홍길동", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
    }

    @Test
    @DisplayName("[지은이]: 이름 형식 파싱")
    void parse_bracketRoleColonNameFormat_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("[지은이]: 이영희");

        assertEquals(1, result.size());
        assertEquals("이영희", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
    }

    @Test
    @DisplayName("쉼표로 구분된 여러 이름 파싱")
    void parse_commaSeparatedNames_returnsMultipleParsed() {
        List<ParsedAuthor> result = strategy.parse("지은이: 김철수, 이영희, 홍길동");

        assertEquals(3, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
        assertEquals("이영희", result.get(1).name());
        assertEquals("홍길동", result.get(2).name());
    }

    @Test
    @DisplayName("중간에 역할이 바뀌는 경우 파싱")
    void parse_roleChangeInMiddle_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("지은이: 김철수, 그림: 이영희");

        assertEquals(2, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
        assertEquals("이영희", result.get(1).name());
        assertEquals("그림", result.get(1).role());
    }

    @Test
    @DisplayName("대괄호 안에서 쉼표로 구분된 여러 이름 파싱")
    void parse_bracketWithCommaSeparatedNames_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("[지은이: 김철수, 이영희]");

        assertEquals(2, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
        assertEquals("이영희", result.get(1).name());
    }

    @Test
    @DisplayName("대괄호 안에서 역할이 바뀌는 경우 파싱")
    void parse_bracketWithRoleChange_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("[지은이: 김철수, 그림: 이영희]");

        assertEquals(2, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
        assertEquals("이영희", result.get(1).name());
        assertEquals("그림", result.get(1).role());
    }

    @Test
    @DisplayName("옮긴이 역할 파싱")
    void parse_translatorRole_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("옮긴이: John Smith");

        assertEquals(1, result.size());
        assertEquals("John Smith", result.get(0).name());
        assertEquals("옮긴이", result.get(0).role());
    }

    @Test
    @DisplayName("엮은이 역할 파싱")
    void parse_editorRole_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("엮은이: 편집자");

        assertEquals(1, result.size());
        assertEquals("편집자", result.get(0).name());
        assertEquals("엮은이", result.get(0).role());
    }

    @Test
    @DisplayName("잘못된 역할 입력 시 빈 리스트 반환")
    void parse_invalidRole_returnsEmptyList() {
        List<ParsedAuthor> result = strategy.parse("잘못된역할: 이름");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("콜론이 없는 경우 빈 리스트 반환")
    void parse_noColon_returnsEmptyList() {
        List<ParsedAuthor> result = strategy.parse("지은이 이름");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("공백만 있는 경우 빈 리스트 반환")
    void parse_whitespaceOnly_returnsEmptyList() {
        List<ParsedAuthor> result = strategy.parse("   ");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("여러 역할 변경 파싱")
    void parse_multipleRoleChanges_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("지은이: 김철수, 그림: 이영희, 옮긴이: 홍길동");

        assertEquals(3, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
        assertEquals("이영희", result.get(1).name());
        assertEquals("그림", result.get(1).role());
        assertEquals("홍길동", result.get(2).name());
        assertEquals("옮긴이", result.get(2).role());
    }

    @Test
    @DisplayName("역할 변경 후 같은 역할이 유지되는 경우")
    void parse_roleChangeThenSameRole_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("지은이: 김철수, 그림: 이영희, 그림: 홍길동");

        assertEquals(3, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
        assertEquals("이영희", result.get(1).name());
        assertEquals("그림", result.get(1).role());
        assertEquals("홍길동", result.get(2).name());
        assertEquals("그림", result.get(2).role());
    }

    @Test
    @DisplayName("[지은이]: 이름, 그림: 이름 형식 파싱")
    void parse_bracketRoleColonNameWithRoleChange_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("[지은이]: 김철수, 그림: 이영희");

        assertEquals(2, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
        assertEquals("이영희", result.get(1).name());
        assertEquals("그림", result.get(1).role());
    }
}
