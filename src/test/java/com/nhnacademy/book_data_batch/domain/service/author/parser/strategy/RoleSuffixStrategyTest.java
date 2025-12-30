package com.nhnacademy.book_data_batch.domain.service.author.parser.strategy;

import com.nhnacademy.book_data_batch.domain.service.author.parser.dto.ParsedAuthor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RoleSuffixStrategy 테스트")
class RoleSuffixStrategyTest {

    private final RoleSuffixStrategy strategy = new RoleSuffixStrategy();

    @Test
    @DisplayName("빈 문자열 입력 시 빈 리스트 반환")
    void parse_emptyString_returnsEmptyList() {
        List<ParsedAuthor> result = strategy.parse("");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("기본 역할 접미사 형식 파싱: 이름 지은이")
    void parse_basicRoleSuffixFormat_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("김철수 지은이");

        assertEquals(1, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
    }

    @Test
    @DisplayName("쉼표로 구분된 여러 이름 파싱")
    void parse_commaSeparatedNames_returnsMultipleParsed() {
        List<ParsedAuthor> result = strategy.parse("김철수, 이영희, 홍길동 지은이");

        assertEquals(3, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
        assertEquals("이영희", result.get(1).name());
        assertEquals("지은이", result.get(1).role());
        assertEquals("홍길동", result.get(2).name());
        assertEquals("지은이", result.get(2).role());
    }

    @Test
    @DisplayName("옮긴이 역할 파싱")
    void parse_translatorRole_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("John Smith 옮긴이");

        assertEquals(1, result.size());
        assertEquals("John Smith", result.get(0).name());
        assertEquals("옮긴이", result.get(0).role());
    }

    @Test
    @DisplayName("엮은이 역할 파싱")
    void parse_editorRole_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("편집자 엮은이");

        assertEquals(1, result.size());
        assertEquals("편집자", result.get(0).name());
        assertEquals("엮은이", result.get(0).role());
    }

    @Test
    @DisplayName("그림 역할 파싱")
    void parse_illustratorRole_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("이영희 그림");

        assertEquals(1, result.size());
        assertEquals("이영희", result.get(0).name());
        assertEquals("그림", result.get(0).role());
    }

    @Test
    @DisplayName("잘못된 역할 입력 시 빈 리스트 반환")
    void parse_invalidRole_returnsEmptyList() {
        List<ParsedAuthor> result = strategy.parse("이름 잘못된역할");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("공백만 있는 경우 빈 리스트 반환")
    void parse_whitespaceOnly_returnsEmptyList() {
        List<ParsedAuthor> result = strategy.parse("   ");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("이름만 있는 경우 빈 리스트 반환")
    void parse_nameWithoutRole_returnsEmptyList() {
        List<ParsedAuthor> result = strategy.parse("김철수");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("이름 앞에 대괄호가 있는 경우 제거 후 파싱")
    void parse_nameWithLeadingBracket_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("[김철수] 지은이");

        assertEquals(1, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
    }

    @Test
    @DisplayName("이름 뒤에 대괄호가 있는 경우 제거 후 파싱")
    void parse_nameWithTrailingBracket_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("김철수] 지은이");

        assertEquals(1, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
    }

    @Test
    @DisplayName("대괄호로 감싸진 이름 파싱")
    void parse_nameInBrackets_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("[김철수, 이영희] 지은이");

        assertEquals(2, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
        assertEquals("이영희", result.get(1).name());
        assertEquals("지은이", result.get(1).role());
    }

    @Test
    @DisplayName("감수 역할 파싱")
    void parse_supervisorRole_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("교수님 감수");

        assertEquals(1, result.size());
        assertEquals("교수님", result.get(0).name());
        assertEquals("감수", result.get(0).role());
    }

    @Test
    @DisplayName("해설 역할 파싱")
    void parse_commentatorRole_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("해설자 해설");

        assertEquals(1, result.size());
        assertEquals("해설자", result.get(0).name());
        assertEquals("해설", result.get(0).role());
    }

    @Test
    @DisplayName("개별 이름에 역할이 포함된 경우")
    void parse_individualNameWithLocalRole_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("김철수 그림, 이영희 그림");

        assertEquals(2, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("그림", result.get(0).role());
        assertEquals("이영희", result.get(1).name());
        assertEquals("그림", result.get(1).role());
    }

    @Test
    @DisplayName("단일 이름에 개별 역할과 글로벌 역할이 모두 있는 경우")
    void parse_singleNameWithLocalAndGlobalRoles_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("김철수 그림 사진");

        assertEquals(2, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("그림", result.get(0).role());
        assertEquals("김철수", result.get(1).name());
        assertEquals("사진", result.get(1).role());
    }

    @Test
    @DisplayName("여러 이름에 다른 역할이 있는 경우")
    void parse_multipleNamesWithDifferentRoles_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("김철수 지은이, 이영희 그림");

        assertEquals(2, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
        assertEquals("이영희", result.get(1).name());
        assertEquals("그림", result.get(1).role());
    }

    @Test
    @DisplayName("연구 역할 파싱")
    void parse_researcherRole_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("연구원 연구");

        assertEquals(1, result.size());
        assertEquals("연구원", result.get(0).name());
        assertEquals("연구", result.get(0).role());
    }

    @Test
    @DisplayName("개발 역할 파싱")
    void parse_developerRole_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("개발자 개발");

        assertEquals(1, result.size());
        assertEquals("개발자", result.get(0).name());
        assertEquals("개발", result.get(0).role());
    }

    @Test
    @DisplayName("기획 역할 파싱")
    void parse_plannerRole_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("기획자 기획");

        assertEquals(1, result.size());
        assertEquals("기획자", result.get(0).name());
        assertEquals("기획", result.get(0).role());
    }

    @Test
    @DisplayName("공백으로 구분된 역할 키워드 파싱")
    void parse_spaceSeparatedRoleKeyword_returnsParsed() {
        List<ParsedAuthor> result = strategy.parse("김철수 공저");

        assertEquals(1, result.size());
        assertEquals("김철수", result.get(0).name());
        assertEquals("지은이", result.get(0).role());
    }
}
