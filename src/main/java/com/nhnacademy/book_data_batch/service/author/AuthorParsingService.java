package com.nhnacademy.book_data_batch.service.author;

import com.nhnacademy.book_data_batch.service.author.dto.AuthorWithRole;
import com.nhnacademy.book_data_batch.service.author.dto.ParseResult;
import com.nhnacademy.book_data_batch.service.author.parser.AuthorRoleParser;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 작가 파싱 서비스. OCP를 따르므로 새 파서는 @Component만 붙이면 자동 등록.
 */
@Service
public class AuthorParsingService {

    private final List<AuthorRoleParser> parsers;

    /** 파서들을 priority로 정렬하여 보관 */
    public AuthorParsingService(List<AuthorRoleParser> parsers) {
        this.parsers = new ArrayList<>(parsers);
        this.parsers.sort(Comparator.comparingInt(AuthorRoleParser::priority));
    }

    /** 작가 필드 파싱 */
    public ParseResult parse(String authorField) {
        if (authorField == null || authorField.isBlank()) {
            return ParseResult.failure();
        }

        for (AuthorRoleParser parser : parsers) {
            ParseResult result = parser.parse(authorField);
            if (result.success()) {
                return result;
            }
        }

        return ParseResult.failure();
    }

    /** 파싱하여 작가 리스트만 반환. 실패 시 빈 리스트 */
    public List<AuthorWithRole> parseToList(String authorField) {
        ParseResult result = parse(authorField);
        return result.success() ? result.authors() : List.of();
    }

    /** 파싱 가능 여부 */
    public boolean canParse(String authorField) {
        return parse(authorField).success();
    }

    /** 등록된 파서 목록 (읽기 전용) */
    public List<AuthorRoleParser> getParsers() {
        return List.copyOf(parsers);
    }

    /** 등록된 파서 수 */
    public int getParserCount() {
        return parsers.size();
    }
}
