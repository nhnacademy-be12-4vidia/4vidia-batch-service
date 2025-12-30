package com.nhnacademy.book_data_batch.domain.service.author.parser.strategy;

import com.nhnacademy.book_data_batch.domain.service.author.parser.dto.ParsedAuthor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ParenthesizedRoleStrategy 테스트")
class ParenthesizedRoleStrategyTest {

    private final ParenthesizedRoleStrategy strategy = new ParenthesizedRoleStrategy();

    @Test
    @DisplayName("빈 문자열 입력 시 빈 리스트 반환")
    void parse_emptyString_returnsEmptyList() {
        List<ParsedAuthor> result = strategy.parse("");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("기본 괄호 형식 파싱: 이름(지은이)")
    void parse_basicParenthesesFormat_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("김철수(지은이)");

        assertEquals(1, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
    }

    @Test
    @DisplayName("쉼표로 구분된 여러 이름 파싱")
    void parse_commaSeparatedNames_returnsMultipleParsed() {
        List<ParsedAuthor> result = strategy.parse("김철수, 이영희(지은이)");

        assertEquals(2, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
        assertEquals("이영희", result.get(1).name());
    }

    @Test
    @DisplayName("여러 역할 그룹 파싱")
    void parse_multipleRoleGroups_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("김철수, 이영희(지은이)홍길동(그림)");

        assertEquals(3, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
        assertEquals("이영희", result.get(1).name());
        assertEquals("지은이", result.get(1).role());
        assertEquals("홍길동", result.get(2).name());
        assertEquals("그림", result.get(2).role());
    }

    @Test
    @DisplayName("옮긴이 역할 파싱")
    void parse_translatorRole_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("John Smith(옮긴이)");

        assertEquals(1, result.size());
        assertEquals("John Smith", result.get(0).name());
        assertEquals("옮긴이", result.get(0).role());
    }

    @Test
    @DisplayName("엮은이 역할 파싱")
    void parse_editorRole_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("편집자(엮은이)");

        assertEquals(1, result.size());
        assertEquals("편집자", result.get(0).name());
        assertEquals("엮은이", result.get(0).role());
    }

    @Test
    @DisplayName("그림 역할 파싱")
    void parse_illustratorRole_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("이영희(그림)");

        assertEquals(1, result.size());
        assertEquals("이영희", result.get(0).name());
        assertEquals("그림", result.get(0).role());
    }

    @Test
    @DisplayName("잘못된 역할 입력 시 빈 리스트 반환")
    void parse_invalidRole_returnsEmptyList() {
        List<ParsedAuthor> result = strategy.parse("이름(잘못된역할)");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("괄호가 없는 경우 빈 리스트 반환")
    void parse_noParentheses_returnsEmptyList() {
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
    @DisplayName("이름 부분에 공백이 있는 경우 파싱")
    void parse_nameWithSpaces_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("John Smith(지은이)");

        assertEquals(1, result.size());
        assertEquals("John Smith", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
    }

    @Test
    @DisplayName("마지막에 남은 문자열이 있는 경우 빈 리스트 반환")
    void parse_remainingTextAfterRoles_returnsEmptyList() {
        List<ParsedAuthor> result = strategy.parse("이름(지은이)추가문자");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("여러 역할 그룹 사이에 공백이 있는 경우 파싱")
    void parse_roleGroupsWithSpaces_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("김철수, 이영희(지은이) 홍길동(그림)");

        assertEquals(3, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("이영희", result.get(1).name());
        assertEquals("홍길동", result.get(2).name());
    }

    @Test
    @DisplayName("감수 역할 파싱")
    void parse_supervisorRole_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("교수님(감수)");

        assertEquals(1, result.size());
        assertEquals("교수님", result.get(0).name());
        assertEquals("감수", result.get(0).role());
    }

    @Test
    @DisplayName("해설 역할 파싱")
    void parse_commentatorRole_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("해설자(해설)");

        assertEquals(1, result.size());
        assertEquals("해설자", result.get(0).name());
        assertEquals("해설", result.get(0).role());
    }

    @Test
    @DisplayName("단일 이름만 있는 경우 빈 리스트 반환")
    void parse_singleNameWithoutRole_returnsEmptyList() {
        List<ParsedAuthor> result = strategy.parse("김철수");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("빈 괄호 형식 빈 리스트 반환")
    void parse_emptyParentheses_returnsEmptyList() {
        List<ParsedAuthor> result = strategy.parse("이름()");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("여러 이름과 하나의 역할 파싱")
    void parse_multipleNamesSingleRole_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("김철수, 이영희, 홍길동(지은이)");

        assertEquals(3, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("이영희", result.get(1).name());
        assertEquals("홍길동", result.get(2).name());
        for (ParsedAuthor author : result) {
            assertEquals("지은이", author.role());
        }
    }
}
