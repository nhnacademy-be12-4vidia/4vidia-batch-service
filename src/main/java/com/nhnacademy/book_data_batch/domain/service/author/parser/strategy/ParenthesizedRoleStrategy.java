package com.nhnacademy.book_data_batch.domain.service.author.parser.strategy;

import com.nhnacademy.book_data_batch.domain.service.author.parser.constant.AuthorRoleMap;
import com.nhnacademy.book_data_batch.domain.service.author.parser.dto.ParsedAuthor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(3)
public class ParenthesizedRoleStrategy implements AuthorParsingStrategy {
    
    // 괄호 안에 들어있는 역할 패턴 (예: "(지은이)")
    private static final Pattern ROLE_PATTERN = Pattern.compile("\\(([^)]+)\\)");

    @Override
    public List<ParsedAuthor> parse(String input) {
        List<ParsedAuthor> results = new ArrayList<>();
        Matcher matcher = ROLE_PATTERN.matcher(input);
        
        int lastEnd = 0;
        boolean foundAny = false;

        while (matcher.find()) {
            foundAny = true;
            // 이전 매칭 끝부터 현재 매칭 시작까지가 이름(들)
            String namePart = input.substring(lastEnd, matcher.start());
            String rolePart = matcher.group(1);
            String role = AuthorRoleMap.getNormalizedRole(rolePart.trim());
            
            if (role != null) {
                // 쉼표로 구분된 이름들 처리
                String[] names = namePart.split(",");
                for (String name : names) {
                    String trimmedName = name.trim();
                    if (!trimmedName.isEmpty()) {
                        results.add(new ParsedAuthor(trimmedName, role));
                    }
                }
            } else {
                // 역할이 유효하지 않으면 이 전략은 실패로 간주
                return Collections.emptyList();
            }
            
            lastEnd = matcher.end();
        }

        if (foundAny) {
            // 남은 문자열이 있는지 확인 (공백이나 쉼표만 남아야 함)
            String remaining = input.substring(lastEnd).replaceAll("[,\\s]+", "");
            if (remaining.isEmpty()) {
                return results;
            }
        }

        return Collections.emptyList();
    }
}
