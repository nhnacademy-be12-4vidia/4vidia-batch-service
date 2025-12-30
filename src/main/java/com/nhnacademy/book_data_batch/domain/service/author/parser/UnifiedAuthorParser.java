package com.nhnacademy.book_data_batch.domain.service.author.parser;

import com.nhnacademy.book_data_batch.domain.service.author.parser.dto.ParsedAuthor;
import com.nhnacademy.book_data_batch.domain.service.author.parser.strategy.AuthorParsingStrategy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class UnifiedAuthorParser implements AuthorParser {

    private final List<AuthorParsingStrategy> strategies;

    public UnifiedAuthorParser(List<AuthorParsingStrategy> strategies) {
        this.strategies = strategies;
    }

    @Override
    public List<ParsedAuthor> parse(String input) {
        if (input == null || input.isBlank()) {
            return Collections.emptyList();
        }

        // 0. 세미콜론(;)으로 구분된 경우 분리하여 처리
        if (input.contains(";")) {
            List<ParsedAuthor> result = new ArrayList<>();
            String[] parts = input.split("\\s*;\\s*");
            for (String part : parts) {
                result.addAll(parse(part.trim()));
            }
            if (!result.isEmpty()) {
                return result;
            }
        }

        String trimmedInput = input.trim();
        for (AuthorParsingStrategy strategy : strategies) {
            List<ParsedAuthor> result = strategy.parse(trimmedInput);
            if (!result.isEmpty()) {
                return result;
            }
        }

        // 모든 전략 실패 시, 원본 필드를 이름으로, 역할을 null로 하는 단일 저자 반환
        return Collections.singletonList(new ParsedAuthor(trimmedInput, null));
    }
}

