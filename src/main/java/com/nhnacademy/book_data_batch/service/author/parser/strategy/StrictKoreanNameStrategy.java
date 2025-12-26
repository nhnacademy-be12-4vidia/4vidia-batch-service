package com.nhnacademy.book_data_batch.service.author.parser.strategy;

import com.nhnacademy.book_data_batch.service.author.parser.dto.ParsedAuthor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(1)
public class StrictKoreanNameStrategy implements AuthorParsingStrategy {
    // 한글 이름만 있는 경우 (2~3글자) 또는 쉼표로 구분된 한글 이름 목록
    private static final Pattern PATTERN = Pattern.compile("^([가-힣]{2,3}(?:,[가-힣]{2,3})*)$");

    @Override
    public List<ParsedAuthor> parse(String input) {
        Matcher matcher = PATTERN.matcher(input);
        if (matcher.matches()) {
            return Arrays.stream(matcher.group(1).split(","))
                    .map(name -> new ParsedAuthor(name.trim(), "지은이"))
                    .toList();
        }
        return Collections.emptyList();
    }
}
